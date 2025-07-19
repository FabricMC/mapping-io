package net.fabricmc.mappingio.format.pdme;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.*;
import java.util.regex.Pattern;

import net.fabricmc.mappingio.MappedElementKind;
import net.fabricmc.mappingio.MappingFlag;
import net.fabricmc.mappingio.MappingUtil;
import net.fabricmc.mappingio.MappingVisitor;
import net.fabricmc.mappingio.format.MappingFormat;
import net.fabricmc.mappingio.tree.MemoryMappingTree;

/**
 * {@linkplain MappingFormat#PDME_FILE Paragraph Delimited Mappings Extended file} reader.
 *
 * <p>Crashes if a second visit pass is requested without
 * {@link MappingFlag#NEEDS_MULTIPLE_PASSES} having been passed beforehand.
 */
public final class PDMEFileReader {

    private static final char DELIM = '\u00B6';

    private PDMEFileReader() {}

    public static void read(Reader in, MappingVisitor visitor) throws IOException {
        Set<MappingFlag> flags = visitor.getFlags();

        MappingVisitor downstream = visitor;
        MemoryMappingTree buffering = null;
        if (flags.contains(MappingFlag.NEEDS_ELEMENT_UNIQUENESS)) {
            buffering = new MemoryMappingTree();
            visitor = buffering;
        }

        BufferedReader br = new BufferedReader(in);
        String header = br.readLine();
        if (header == null) return;

        List<String> lines = new ArrayList<String>();
        for (String l; (l = br.readLine()) != null; ) {
            lines.add(l);
        }

        boolean multi = flags.contains(MappingFlag.NEEDS_MULTIPLE_PASSES);

        while (true) {
            boolean h = visitor.visitHeader();
            if (h) {
                visitor.visitNamespaces(MappingUtil.NS_SOURCE_FALLBACK,
                        Collections.singletonList(MappingUtil.NS_TARGET_FALLBACK));
            }

            if (visitor.visitContent()) {
                parseContent(lines, visitor);
            }

            if (visitor.visitEnd()) break;
            if (!multi) {
                throw new IllegalStateException("Repeated visitation requested without NEEDS_MULTIPLE_PASSES");
            }
        }

        if (buffering != null) {
            buffering.accept(downstream);
        }
    }

    private static abstract class Row {
        abstract void emit(MappingVisitor v) throws IOException;
    }

    private static final class ClassRow extends Row {
        final String original;   // dotted
        final String mappedCol;  // raw column value (may be null)
        final String comment;
        String finalMapped;      // resolved after all class rows collected
        boolean resolved;

        ClassRow(String o, String m, String c) {
            this.original = o;
            this.mappedCol = m;
            this.comment = c;
        }

        void resolve(Map<String, ClassRow> byOriginal) {
            if (resolved) return;
            resolved = true;
            if (original == null) return;

            // Top-level?
            int lastDollar = original.lastIndexOf('$');
            if (lastDollar < 0) {
                // Top-level
                if (mappedCol != null) {
                    finalMapped = mappedCol;
                } else {
                    finalMapped = original;
                }
                return;
            }

            // Inner class
            String outerOrig = original.substring(0, lastDollar);
            String tailOrig  = original.substring(lastDollar + 1);

            ClassRow outerRow = byOriginal.get(outerOrig);
            if (outerRow != null) {
                outerRow.resolve(byOriginal);
            }

            String outerFinal = outerRow != null && outerRow.finalMapped != null
                    ? outerRow.finalMapped
                    : outerOrig;

            if (mappedCol == null) {
                // Propagate outer rename + original tail
                finalMapped = outerFinal + '$' + tailOrig;
            } else {
                if (isSimpleTail(mappedCol)) {
                    // Tail-only rename
                    finalMapped = outerFinal + '$' + mappedCol;
                } else {
                    // Explicit full mapped path
                    finalMapped = mappedCol;
                }
            }
        }

        private static boolean isSimpleTail(String s) {
            return s.indexOf('$') < 0 && s.indexOf('.') < 0;
        }

        @Override
        void emit(MappingVisitor v) throws IOException {
            if (original == null) return;
            if (finalMapped == null) finalMapped = original;

            String slashOrig = original.replace('.', '/');
            boolean identity = original.equals(finalMapped);
            String slashMapped = identity ? null : finalMapped.replace('.', '/');

            if (v.visitClass(slashOrig)) {
                if (slashMapped != null) {
                    v.visitDstName(MappedElementKind.CLASS, 0, slashMapped);
                }
                if (v.visitElementContent(MappedElementKind.CLASS) && comment != null) {
                    v.visitComment(MappedElementKind.CLASS, comment);
                }
            }
        }
    }

    private static final class FieldRow extends Row {
        final String original; // cls.field:desc
        final String mapped;
        final String comment;

        FieldRow(String o, String m, String c) {
            this.original = o;
            this.mapped = m;
            this.comment = c;
        }

        @Override
        void emit(MappingVisitor v) throws IOException {
            if (original == null) return;
            int colon = original.lastIndexOf(':');
            if (colon < 0) return;
            String desc = original.substring(colon + 1);
            String ownerAndName = original.substring(0, colon);
            int lastDot = ownerAndName.lastIndexOf('.');
            if (lastDot < 0) return;
            String clsDotted = ownerAndName.substring(0, lastDot);
            String fieldName = ownerAndName.substring(lastDot + 1);
            String slashClass = clsDotted.replace('.', '/');

            if (v.visitClass(slashClass)) {
                v.visitElementContent(MappedElementKind.CLASS);
            }
            if (v.visitField(fieldName, desc)) {
                if (mapped != null) {
                    v.visitDstName(MappedElementKind.FIELD, 0, mapped);
                }
                if (v.visitElementContent(MappedElementKind.FIELD) && comment != null) {
                    v.visitComment(MappedElementKind.FIELD, comment);
                }
            }
        }
    }

    private static final class MethodRow extends Row {
        final String original; // cls.method(desc)
        final String mapped;
        final String comment;

        MethodRow(String o, String m, String c) {
            this.original = o;
            this.mapped = m;
            this.comment = c;
        }

        @Override
        void emit(MappingVisitor v) throws IOException {
            if (original == null) return;
            int paren = original.indexOf('(');
            if (paren < 0) return;
            int lastDot = original.lastIndexOf('.', paren);
            if (lastDot < 0) return;
            String clsDotted = original.substring(0, lastDot);
            String methodName = original.substring(lastDot + 1, paren);
            String methodDesc = original.substring(paren);
            String slashClass = clsDotted.replace('.', '/');

            if (v.visitClass(slashClass)) {
                v.visitElementContent(MappedElementKind.CLASS);
            }
            if (v.visitMethod(methodName, methodDesc)) {
                if (mapped != null) {
                    v.visitDstName(MappedElementKind.METHOD, 0, mapped);
                }
                if (v.visitElementContent(MappedElementKind.METHOD) && comment != null) {
                    v.visitComment(MappedElementKind.METHOD, comment);
                }
            }
        }
    }

    private static final class ParamRow extends Row {
        final String def;
        final String posStr;
        final String mappedName;
        final String comment;
        final String originalMeta; // for locals: lvtRowIndex:lvIndex:start:end

        ParamRow(String d, String p, String m, String c, String om) {
            this.def = d;
            this.posStr = p;
            this.mappedName = m;
            this.comment = c;
            this.originalMeta = om;
        }

        @Override
        void emit(MappingVisitor v) throws IOException {
            if (def == null || posStr == null || mappedName == null) return;

            int paren = def.indexOf('(');
            if (paren < 0) return;
            int lastDot = def.lastIndexOf('.', paren);
            if (lastDot < 0) return;

            String clsDotted = def.substring(0, lastDot);
            String methodName = def.substring(lastDot + 1, paren);
            String methodDesc = def.substring(paren);

            int pos;
            try {
                pos = Integer.parseInt(posStr);
            } catch (NumberFormatException e) {
                return;
            }
            if (pos <= 0) return;

            String slashClass = clsDotted.replace('.', '/');

            if (v.visitClass(slashClass)) {
                v.visitElementContent(MappedElementKind.CLASS);
            }
            if (v.visitMethod(methodName, methodDesc)) {
                v.visitElementContent(MappedElementKind.METHOD);
            }

            int paramCount = countParams(methodDesc);
            if (pos <= paramCount) {
                int zero = pos - 1;
                if (v.visitMethodArg(zero, -1, mappedName)) {
                    v.visitDstName(MappedElementKind.METHOD_ARG, 0, mappedName);
                    if (v.visitElementContent(MappedElementKind.METHOD_ARG) && comment != null) {
                        v.visitComment(MappedElementKind.METHOD_ARG, comment);
                    }
                }
            } else {
                if (originalMeta == null) return;
                String[] parts = originalMeta.split(":");
                if (parts.length != 4) return;
                try {
                    int lvtRowIndex = Integer.parseInt(parts[0]);
                    int lvIndex = Integer.parseInt(parts[1]);
                    int start = Integer.parseInt(parts[2]);
                    int end = Integer.parseInt(parts[3]);
                    if (v.visitMethodVar(lvtRowIndex, lvIndex, start, end, mappedName)) {
                        v.visitDstName(MappedElementKind.METHOD_VAR, 0, mappedName);
                        if (v.visitElementContent(MappedElementKind.METHOD_VAR) && comment != null) {
                            v.visitComment(MappedElementKind.METHOD_VAR, comment);
                        }
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }
    }

    private static void parseContent(List<String> lines, MappingVisitor v) throws IOException {
        Map<String, ClassRow> classRowsByOrig = new LinkedHashMap<String, ClassRow>();
        List<Row> nonClassRows = new ArrayList<Row>();

        for (int i = 0; i < lines.size(); i++) {
            String raw = lines.get(i);
            if (raw == null) continue;
            String trimmed = raw.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;

            String[] cols = splitToSix(trimmed);
            String tipo = cols[0];
            String original = normalizeEmpty(cols[1]);
            String nuevo    = normalizeEmpty(cols[2]);
            String def      = normalizeEmpty(cols[3]);
            String pos      = normalizeEmpty(cols[4]);
            String desc     = normalizeEmpty(cols[5]);

            if ("Class".equals(tipo)) {
                ClassRow cr = new ClassRow(original, nuevo, desc);
                classRowsByOrig.put(original, cr);
            } else if ("Var".equals(tipo)) {
                nonClassRows.add(new FieldRow(original, nuevo, desc));
            } else if ("Def".equals(tipo)) {
                nonClassRows.add(new MethodRow(original, nuevo, desc));
            } else if ("Param".equals(tipo)) {
                nonClassRows.add(new ParamRow(def, pos, nuevo, desc, original));
            }
        }

        // Parse Subclases
        for (ClassRow cr : classRowsByOrig.values()) {
            cr.resolve(classRowsByOrig);
        }

        // Emit classes first
        for (ClassRow cr : classRowsByOrig.values()) {
            cr.emit(v);
        }

        for (Row r : nonClassRows) {
            r.emit(v);
        }
    }


    private static String[] splitToSix(String line) {
        String[] arr = line.split(Pattern.quote(String.valueOf(DELIM)), -1);
        if (arr.length == 6) return arr;
        if (arr.length < 6) {
            String[] six = new String[6];
            System.arraycopy(arr, 0, six, 0, arr.length);
            for (int i = arr.length; i < 6; i++) six[i] = "";
            return six;
        }
        // collapse extra columns into desc
        StringBuilder desc = new StringBuilder(arr[5]);
        for (int i = 6; i < arr.length; i++) {
            if (desc.length() > 0) desc.append(DELIM);
            desc.append(arr[i]);
        }
        String[] six = new String[6];
        System.arraycopy(arr, 0, six, 0, 5);
        six[5] = desc.toString();
        return six;
    }

    private static String normalizeEmpty(String s) {
        return (s == null || s.isEmpty() || "nil".equals(s)) ? null : s;
    }

    private static int countParams(String desc) {
        if (desc == null || desc.length() == 0) return 0;
        int count = 0;
        int i = 1; // skip '('
        while (i < desc.length()) {
            char c = desc.charAt(i);
            if (c == ')') break;
            if (c == 'L') {
                int semi = desc.indexOf(';', i);
                if (semi < 0) break;
                i = semi + 1;
                count++;
            } else if (c == '[') {
                i++;
                while (i < desc.length() && desc.charAt(i) == '[') i++;
                if (i < desc.length() && desc.charAt(i) == 'L') {
                    int semi = desc.indexOf(';', i);
                    if (semi < 0) break;
                    i = semi + 1;
                } else {
                    i++;
                }
                count++;
            } else {
                i++; // primitive
                count++;
            }
        }
        return count;
    }
}
