package io.rappt.layout;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Iterables;
import com.google.common.collect.Table;
import org.apache.commons.lang3.StringUtils;
import org.joox.Match;
import io.rappt.antlr.AMLErrorReporter;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.joox.JOOX.$;

public class Model implements AMLErrorReporter {

    public static final int FIRST_COL = 0;
    public static final int FIRST_ROW = 0;
    public final int lastRow;

    public Table<Integer, Integer, ViewElement> data = HashBasedTable.create();
    public Map<Integer, RowData> rowData = new HashMap<>();
    private Document document;
    private Match match;

    public Model(String layout, Path inputXmlFile) throws IOException, SAXException {
        this.data = populateData(layout);
        this.lastRow = data.size();
        document = $(inputXmlFile.toFile()).document();
        match = $(document).namespace("android", "http://schemas.android.com/apk/res/android");
        rewriteDocument();
        analyzeRows();
    }

    void printDoc() {
        System.out.println("---Start Doc---");
        try {
            $(document).write(System.out);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("---End Doc---");
    }

    private void rewriteDocument() {
        Match newMatch = match;
        if ($(document).tag().equals("LinearLayout")) {
            newMatch = match
                    .wrap("RelativeLayout").parent()
                    .attr("xmlns:android", "http://schemas.android.com/apk/res/android")
                    .attr("android:padding", "10dp")
                    .attr("android:layout_width", "match_parent")
                    .attr("android:layout_height", "wrap_content");
        }
        for (Table.Cell<Integer, Integer, ViewElement> i : data.cellSet()) {
            Match element = match.xpath(id(i.getValue().id));
            $(document).append(element);
        }
        $(document).find("LinearLayout").remove();
        match = newMatch.namespace("android", "http://schemas.android.com/apk/res/android");
    }

    private void analyzeRows() {
        for (Map.Entry<Integer, Map<Integer, ViewElement>> entry : data.rowMap().entrySet()) {
            int row = entry.getKey();
            RowData data = new RowData();
            if (rowIsMultiLine(row)) {
                data.rowType = RowData.ROW_TYPE.MULTI_LINE;
            } else if (entry.getValue().get(0).id.isEmpty()) {
                data.rowType = RowData.ROW_TYPE.SINGLE_ALIGN_RIGHT;
            } else if (entry.getValue().size() > 1) {
                data.rowType = RowData.ROW_TYPE.FLAT;
            } else {
                data.rowType = RowData.ROW_TYPE.STANDARD;
            }
            data.highestId = calculateGreatestHeight(entry.getValue());
            rowData.put(row, data);
        }
    }

    private String calculateGreatestHeight(Map<Integer, ViewElement> cols) {
        String id = cols.get(FIRST_COL).id;
        for (ViewElement ve : cols.values()) {
            String tag = match.xpath(id(ve.id)).tag();
            if ("ImageView".equals(tag)) {
                id = ve.id;
                break;
            }
            if ("Button".equals(tag)) {
                id = ve.id;
            }
        }
        return id;
    }


    public boolean applyRow() {
        boolean hasErrors = false;
        for(int row : rowData.keySet()) {
            RowData data = rowData.get(row);
            if (data.isFlat())
                newHorizontalRow(row);
            if (data.isAlignRight())
                newRelativeRow(row);
            if (data.isMultiLine())
                hasErrors = newMultiLineRow(row);
            //if (data.isStandard())
            if (hasErrors) break;
        }
        return hasErrors;
    }

    // return false if no errors
    private boolean newMultiLineRow(Integer row) {
        boolean hasErrors = false;
        String highest = rowData.get(row).highestId;
        ViewElement firstElement = data.get(row, FIRST_COL);
        int lastCol = data.rowMap().get(row).size() - 1;
        ViewElement lastElement = data.get(row, lastCol);
        boolean fromRightToLeft = false;
        if (firstElement.id.equals(highest)) {
            fromRightToLeft = true;
            firstElement.addAlignLeft();
        } else if (lastElement.id.equals(highest)) {
            lastElement.addAlignRight();
        } else {
            String error = "Large items should be on the edges";
            addError(error);
            hasErrors = true;
        }
        if (!hasErrors) {
            String previousId = "";
            for (Map.Entry<Integer, ViewElement> element : data.rowMap().get(row).entrySet()) {
                ViewElement vE = element.getValue();
                if (!vE.id.equals(highest)) {
                    if (row > FIRST_ROW) {
                        if (element.getKey() == FIRST_COL || (fromRightToLeft && element.getKey() == FIRST_COL + 1))
                            previousId = previousId(vE.id, row);
                        vE.addAttrLayoutBelow(previousId);
                    }
                    if (fromRightToLeft) {
                        if (element.getKey() == FIRST_COL + 1) {
                            vE.addAlignRightTo(highest);
                        } else {
                            vE.addAlignRightTo(data.rowMap().get(row).get(element.getKey() - 1).id);
                        }
                    } else {
                        if (element.getKey() != FIRST_COL)
                            vE.addAlignRightTo(data.rowMap().get(row).get(element.getKey() - 1).id);
                    }
                } else {
                    if (isTopRow(row, element.getKey(), highest, lastCol)) {
                        vE.addAttrLayoutBelow(rowData.get(row - 1).highestId);
                    }
                }
            }
        }
        return hasErrors;
    }


    boolean isTopRow(int row, int col, String highest, int lastCol) {
        int nextLast = data.row(row + 1).size() - 1;
        return (col == lastCol && row < lastRow && data.get(row + 1, nextLast) != null && data.get(row + 1, nextLast).id.equals(highest)) ||
                (row > FIRST_ROW && row < lastRow && data.get(row + 1, col) != null && data.get(row + 1, col).id.equals(highest));
    }

    private void newRelativeRow(int row) {
        if (row > FIRST_ROW) {
            //String previousId = data.get(row-1, FIRST_COL).id;
            Map<Integer, ViewElement> rowMap = data.rowMap().get(row);
            int colLast = rowMap.size();
            for (Map.Entry<Integer, ViewElement> entry : rowMap.entrySet()) {
                String previousId = previousId(entry.getValue().id, row);
                entry.getValue().addAttrLayoutBelow(previousId);
                int col = entry.getKey();
                if (col < colLast - 1) {
                    entry.getValue().addAlignLeftTo(rowMap.get(col + 1).id);
                } else {
                    entry.getValue().addAlignRight();
                }
                String highestId = rowData.get(row).highestId;
                if (!entry.getValue().id.equals(highestId)) {
                    entry.getValue().addRelativeCenterContent(highestId);
                }
            }
        }
    }

    boolean rowIsMultiLine(int row) {
        Map<Integer, ViewElement> rowData = data.row(row);
        boolean isMultiLine = false;
        int lastRow = Iterables.getLast(data.rowKeySet());
        for (Integer col : rowData.keySet()) {
            String currentId = rowData.get(col).id;
            int lastCol = rowData.size() - 1;
            if (row > Model.FIRST_ROW) {
                if (data.get(row - 1, col) != null)
                    isMultiLine = currentId.equals(data.get(row - 1, col).id);
            }
            if (row < lastRow && !isMultiLine) {
                if (col == lastCol) {
                    int lastColNextRow = data.row(row + 1).size() - 1;
                    isMultiLine = currentId.equals(data.get(row + 1, lastColNextRow).id);
                }
                if (data.get(row + 1, col) != null)
                    isMultiLine = currentId.equals(data.get(row + 1, col).id);
            }
            if (row == lastRow && !isMultiLine) {
                if (col == lastCol) {
                    int lastColNextRow = data.row(row - 1).size() - 1;
                    if (data.get(row - 1, lastColNextRow) != null) {
                        isMultiLine = currentId.equals(data.get(row - 1, lastColNextRow).id);
                    }
                }
            }
            if (isMultiLine) break;
        }
        return isMultiLine;
    }

    public void writeXmlFile(Path outputFile) throws IOException {
        $(document).write(outputFile.toFile());
    }

    public Match elementWithId(String id) {
        return match.xpath("//*[@android:id='@+id/" + id + "']");
    }

    // Apply attributes to elements
    public void updateDocument() {
        for (Table.Cell<Integer, Integer, ViewElement> cell : data.cellSet()) {
            Match element = elementWithId(cell.getValue().id);
            cell.getValue().getAttrs().forEach(element::attr);
        }
    }

    private Table<Integer, Integer, ViewElement> populateData(String layout) {
        Table<Integer, Integer, ViewElement> data = HashBasedTable.create();
        int rowIndex = 0;
        int paddingRows = 0;
        for (String row : layout.split(">")) {

            if (row.isEmpty()) {
                if (rowIndex == 0) continue;
                paddingRows++;

            } else {
                int colIndex = 0;
                if (row.contains("|")) {
                    for (String col : row.split("\\|")) {
                        data.put(rowIndex, colIndex, new ViewElement(StringUtils.strip(col), paddingRows));
                        colIndex++;
                    }
                } else {
                    data.put(rowIndex, colIndex, new ViewElement(StringUtils.strip(row), paddingRows));
                }
                rowIndex++;
                paddingRows = 0;
            }
        }
        return data;
    }

    void newHorizontalRow(int row) {
        String firstId = data.get(row, FIRST_COL).id;
        String ids = buildXpathIds(row);
        String newRowId = "@+id/row" + row;
        Match newMatch = match.xpath(id(firstId))
                .wrap("LinearLayout").parent()
                .attr("android:id", newRowId)
                .attr("android:orientation", "horizontal")
                .attr("android:layout_width", "match_parent")
                .attr("android:layout_height", "wrap_content")
                .append(match.xpath(ids));
        if (row > FIRST_ROW) {
            String prevId = "";
            if (!rowData.get(row - 1).isFlat()) {
                prevId = rowData.get(row - 1).highestId;
            } else {
                prevId = newMatch.prev().attr("id");
            }
            newMatch.attr("android:layout_below", ViewElement.droidId(prevId));
        }
    }

    private String buildXpathIds(int row) {
        StringBuilder builder = new StringBuilder();
        builder.append("//*[");
        builder.append("@android:id='@+id/" + data.get(row, FIRST_COL).id + "'");
        for (Map.Entry<Integer, ViewElement> entry : data.rowMap().get(row).entrySet()) {
            if (entry.getKey() == FIRST_COL) continue;
            String id = entry.getValue().id;
            builder.append(" or @android:id='@+id/" + id + "'");
        }
        builder.append("]");
        return builder.toString();
    }

    private String id(String id) {
        return "//*[@android:id='@+id/" + id + "']";
    }

    public String previousId(String id, int row) {
        String response = "";
        Match element = match.xpath(id(id));
        if (element.isNotEmpty() && !element.parent().tag().equals("LinearLayout")) {
            if (rowData.get(row - 1).isFlat()) {
                int prevRowIndex = row - 1;
                response = "row" + prevRowIndex;
            } else if (rowData.get(row).isMultiLine()) {
                response = element.prev().attr("id");
                if (response.equals("@+id/" + rowData.get(row).highestId)) {
                    response = element.prev().prev().attr("id");
                }
            } else if (!rowData.get(row).isFlat()) {
                response = "@+id/" + rowData.get(row - 1).highestId;
            } else {
                response = element.prev().attr("id");
            }
        }
        return response;
    }

    void applyRowValues() {
        data.cellSet().stream().filter(cell -> cell.getRowKey() > Model.FIRST_ROW).forEach(cell -> {
            String previousId = previousId(cell.getValue().id, cell.getRowKey());
            if (!previousId.isEmpty() && rowData.get(cell.getRowKey()).isStandard())
                cell.getValue().addAttrLayoutBelow(previousId);
        });

//        data.cellSet().stream()
//                .filter(cell -> rowData.get(cell.getRowKey()).isStandard() && data.rowMap().get(cell.getRowKey()).size() == 1)
//                .forEach(cell -> {
//                    ViewElement vE = cell.getValue();
//                    Match element = match.xpath(id(vE.id));
//                    if ("ImageView".equals(element.tag())) {
//                        vE.addRelativeCenter();
//                    }
//                });

    }

    double layoutWeight(int row, int col) {
        String id = data.get(row, col).id;
        double length = data.rowMap().get(row).size();
        double repeatIds = 0;
        for (Map.Entry<Integer, ViewElement> entry : data.rowMap().get(row).entrySet()) {
            if (id.equals(entry.getValue().id)) {
                repeatIds++;
            }
        }
        return repeatIds / length;
    }

    void addLinearAttributes() {
        data.rowMap().entrySet().stream()
                .filter(cell -> rowData.get(cell.getKey()).isFlat())
                .forEach(cell -> {
                    int row = cell.getKey();
                    cell.getValue().forEach((col, vE) -> {
                        if (!match.xpath(id(vE.id)).tag().equals("ImageView"))
                            vE.addWidthWeight(layoutWeight(row, col));
                        vE.addLinearCenterContent();
                    });
                });


    }

}
