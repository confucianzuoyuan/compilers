package Tree;

// SEQ(s1, s2) 语句s1之后跟随语句s2。
public class SEQ extends Stm {
    public Stm left, right;

    public SEQ(Stm l, Stm r) {
        left = l;
        right = r;
    }

    public ExpList kids() {
        throw new Error("kids() not applicable to SEQ");
    }

    public Stm build(ExpList kids) {
        throw new Error("build() not applicable to SEQ");
    }
}
