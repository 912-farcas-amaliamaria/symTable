package org.example;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

@AllArgsConstructor
@Getter
@Setter
public class SymbolTable {
    private ArrayList<ArrayList<Pair<Object, Tuple>>> elements = new ArrayList<>();
    private int size = 13;

    public SymbolTable() {
        for(int i=0; i<size; i++) {
            this.elements.add(new ArrayList<>());
        }
    }

    public Tuple add(String value) {
        Tuple searchTuple = this.searchByElement(value);
        if(searchTuple.equals(new Tuple(-1, -1))){
            int hashCode =  Math.abs(value.hashCode() % size);
            Tuple tuple =  new Tuple(hashCode, elements.get(hashCode).size());
            elements.get(hashCode).add(new Pair<>(value, tuple));
            return tuple;
        }
        return searchTuple;
    }

    public Tuple searchByElement(String value) {
        ArrayList<Pair<Object, Tuple>> listElements = elements.get(Math.abs(value.hashCode()% size));
        if(listElements.isEmpty()){
            return new Tuple(-1, -1);
        }

        for (Pair<Object, Tuple> pair : listElements) {
            if(Objects.equals(pair.getKey(), value)){
                return pair.getValue();
            }
        }
        return new Tuple(-1, -1);
    }

    public Object searchByCode(Tuple code){
        int hashCode = code.getHash();
        int index = code.getPos();

        ArrayList<Pair<Object, Tuple>> listAtHash = elements.get(hashCode);

        return listAtHash.get(index).getKey();

    }

    public void writeToFile(String filePath){
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write(this.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public String toString() {
        StringBuilder table = new StringBuilder("element            code \n");
        for (ArrayList<Pair<Object, Tuple>> array:
                elements) {
            if(!array.isEmpty()){
                for (Pair<Object, Tuple> pair: array) {
                    table.append(pair.toStringPIF());
                }
            }
        }
        return table.toString();
    }
}
