package com.reelvault.util;

/**
 * Utility class for converting between Java float[] and the PostgreSQL pgvector
 * text representation format: "[f1,f2,f3,...,f384]"
 *
 * <p>PostgreSQL's vector type accepts string literals in this bracket format,
 * so we store embeddings as TEXT in the entity while Hibernate sends/receives
 * the string representation. PostgreSQL auto-casts text to vector on insert/update.
 *
 * <p>Example: float[]{0.1f, 0.2f, 0.3f} → "[0.1,0.2,0.3]"
 */
public final class VectorUtils {

    private VectorUtils() {
        // Utility class — no instances
    }

    /**
     * Convert a float[] embedding to the PostgreSQL vector string literal.
     *
     * @param vector the embedding array (e.g. 384-dim from all-MiniLM-L6-v2)
     * @return string like "[0.1,0.2,0.3,...]"
     */
    public static String toVectorString(float[] vector) {
        if (vector == null || vector.length == 0) {
            return null;
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(vector[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Parse a PostgreSQL vector string literal back to a float[].
     *
     * @param vectorString string like "[0.1,0.2,0.3,...]"
     * @return float[] embedding
     */
    public static float[] fromVectorString(String vectorString) {
        if (vectorString == null || vectorString.isBlank()) {
            return null;
        }
        // Remove surrounding brackets: "[0.1,0.2,...]" → "0.1,0.2,..."
        String stripped = vectorString.trim();
        if (stripped.startsWith("[")) stripped = stripped.substring(1);
        if (stripped.endsWith("]"))   stripped = stripped.substring(0, stripped.length() - 1);

        String[] parts = stripped.split(",");
        float[] result = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            // MED-3: Provide context on parse failures so corruption is diagnosable
            try {
                result[i] = Float.parseFloat(parts[i].trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                    "Corrupt vector string at index " + i + ": could not parse '" + parts[i].trim() + "'", e
                );
            }
        }
        return result;
    }
}
