package dmgcalculator.entities;

public class Range {

    private int min;
    private int max;

    public Range() {
        this(0);
    }

    public Range(int value) {
        this(value, value);
    }

    public Range(int min, int max) {
        this.min = min;
        this.max = max;
    }

    public void set(int min, int max) {
        this.min = min;
        this.max = max;
    }

    public void set(int value) {
        this.set(value, value);
    }

    public int getMin() {
        return min;
    }

    public int getMax() {
        return max;
    }

    public int getValue() {
        return max;
    }

    public boolean isConstantsValue() {
        return min == max;
    }
}
