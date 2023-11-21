package org.example;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@AllArgsConstructor
@Getter
@Setter
public class Tuple {
    private Integer hash;
    private Integer pos;

    @Override
    public String toString() {
        return "(" + hash +
                ", " + pos +
                ')';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Tuple otherTuple = (Tuple) obj;
        return Objects.equals(hash, otherTuple.hash) && Objects.equals(pos, otherTuple.pos);
    }
}
