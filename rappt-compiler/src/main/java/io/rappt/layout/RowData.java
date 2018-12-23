package io.rappt.layout;

public class RowData {

    public ROW_TYPE rowType;
    public String highestId = "";
    public int rowTopPaddingMultiplier = 0;

    public boolean isMultiLine() {
        return rowType == ROW_TYPE.MULTI_LINE;
    }

    public boolean isStandard() {
        return rowType == ROW_TYPE.STANDARD;
    }

    enum ROW_TYPE {
        FLAT, SINGLE_ALIGN_RIGHT, MULTI_LINE, STANDARD
    }

    boolean isFlat() {
        return rowType == ROW_TYPE.FLAT;
    }

    boolean isAlignRight() {
        return rowType == ROW_TYPE.SINGLE_ALIGN_RIGHT;
    }
}
