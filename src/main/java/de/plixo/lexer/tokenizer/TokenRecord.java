package de.plixo.lexer.tokenizer;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class TokenRecord<T> {
    public final T token;
    public final String data;

    @Override
    public String toString() {
        return "TokenRecord{" +
                "token=" + token +
                ", data='" + data + '\'' +
                '}';
    }
}
