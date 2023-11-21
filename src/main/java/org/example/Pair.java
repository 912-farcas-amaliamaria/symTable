package org.example;

import lombok.*;

import java.util.Objects;


@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Pair<T1, T2> {
    private T1 key;
    private T2 value;


    public String toStringPIF() {
        return key + "      " + value + " \n";
    }

    @Override
    public String toString() {
        return "(" + key +
                ", " + value +
                ')';
    }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Pair<?, ?> pair = (Pair<?, ?>) o;
            return Objects.equals(key, pair.key) && Objects.equals(value, pair.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(key, value);
        }
}
