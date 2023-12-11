package org.example;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ParseTable {
    private Map<Pair<String, String>, List<String>> table = new HashMap<>();

    public void put(Pair<String, String> key, List<String> value) {
        table.put(key, value);
    }

    public List<String> get(Pair<String, String> key) {
        for (Map.Entry<Pair<String, String>, List<String>> entry : table.entrySet()) {
            if (entry.getValue() != null) {
                Pair<String, String> currentKey = entry.getKey();
                List<String> currentValue = entry.getValue();

                if (currentKey.getKey().equals(key.getKey()) && currentKey.getValue().equals(key.getValue())) {
                    return currentValue;
                }
            }
        }

        return null;
    }

    public boolean containsKey(Pair<String, String> key) {
        boolean result = false;
        for (Pair<String, String> currentKey : table.keySet()) {
            if (currentKey.getKey().equals(key.getKey()) && currentKey.getValue().equals(key.getValue())) {
                result = true;
            }
        }

        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        for (Map.Entry<Pair<String, String>,List<String>> entry : table.entrySet()) {
            if (entry.getValue() != null) {
                Pair<String, String> key = entry.getKey();
                List<String> value = entry.getValue();

                sb.append("M[").append(key.getKey()).append(",").append(key.getValue()).append("] = [")
                        .append(value).append("]\n");
            }
        }

        writeToFile(sb);
        return sb.toString();
    }

    public static void writeToFile(StringBuilder stringBuilder) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("parserOut.txt"))) {
            writer.write(stringBuilder.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}