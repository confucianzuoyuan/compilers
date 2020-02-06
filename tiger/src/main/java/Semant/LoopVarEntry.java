package Semant;

import Translate.Access;
import Types.Type;

public class LoopVarEntry extends VarEntry {
    public LoopVarEntry(Type ty, Access acc) {
        super(ty, acc);
    }

    public LoopVarEntry(Type ty, Access acc, boolean isf) {
        super(ty, acc, isf);
    }
}
