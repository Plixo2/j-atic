package de.plixo.atic.lexer;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TextRegion {
    final String lines;


    public static class Position {

        final int from;
        final int to;
        final TextRegion region;

        public Position(int from, int to, TextRegion region) {
            this.from = Math.min(from, to);
            this.to = Math.max(from, to);
            this.region = region;
        }

        public Position combine(Position next) {
            if (next.region != this.region) {
                throw new IncompatibleRegionException();
            }
            return new Position(this.from, next.to, this.region);
        }


        public Position extendLeft(int amount) {
            return new Position(this.from - amount, this.to, this.region);
        }

        public Position extendRight(int amount) {
            return new Position(this.from, this.to + amount, this.region);
        }

        public String getString() {
            return region.content(this.from, this.from - this.to);
        }

    }

    public String content(int position, int length) {
//        int subLength = 0;
//        for (String line : lines) {
//            final int thisLength = line.length();
//            if (position > subLength + thisLength) {
//                subLength += thisLength;
//            } else {
//                final int subLine = position - subLength;
//                if (subLine + length > line.length()) {
//                    throw new IndexOutOfBoundsException();
//                }
//                return line.substring(subLine, subLine + length);
//            }
//        }
//        throw new IndexOutOfBoundsException();
        return lines.substring(position, position + length);
    }

    public static class IncompatibleRegionException extends RuntimeException {
    }

}
