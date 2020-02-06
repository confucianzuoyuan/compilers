package Tree;

public abstract class Stm {
    public abstract ExpList kids();

    public abstract Stm build(ExpList kids);
}
