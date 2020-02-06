package Tree;

import Temp.Label;

// NAME(n) 符号常数n(相当于汇编语言中的标号)
public class NAME extends Exp {
    public Label label;

    public NAME(Label l) {
        super(null);
        label = l;
    }

    public ExpList kids() {
        return null;
    }

    public Exp build(ExpList kids) {
        return this;
    }
}
