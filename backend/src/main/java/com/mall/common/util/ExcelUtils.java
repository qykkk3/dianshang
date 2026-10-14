package com.mall.common.util;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Excel 导出工具（基于 Apache POI）
 *
 * <p>用于商品、订单等数据的批量导出，以及导入模板下载。</p>
 */
public final class ExcelUtils {

    private ExcelUtils() {
    }

    /**
     * 导出 Excel 到响应流
     *
     * @param response 响应
     * @param fileName 文件名（不含扩展名）
     * @param sheetName 工作表名
     * @param headers 表头
     * @param rows    数据行（与表头一一对应，null 输出空单元格）
     */
    public static void export(HttpServletResponse response, String fileName, String sheetName,
                              String[] headers, List<Object[]> rows) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(sheetName);
            CellStyle headerStyle = buildHeaderStyle(workbook);
            CellStyle bodyStyle = buildBodyStyle(workbook);

            Row headerRow = sheet.createRow(0);
            headerRow.setHeightInPoints(22);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, Math.min(40, Math.max(12, headers[i].length() * 4 + 6)) * 256);
            }

            for (int r = 0; r < rows.size(); r++) {
                Row row = sheet.createRow(r + 1);
                Object[] data = rows.get(r);
                for (int c = 0; c < headers.length; c++) {
                    Cell cell = row.createCell(c);
                    Object value = c < data.length ? data[c] : null;
                    if (value == null) {
                        cell.setCellValue("");
                    } else if (value instanceof Number number) {
                        cell.setCellValue(number.doubleValue());
                    } else {
                        cell.setCellValue(String.valueOf(value));
                    }
                    cell.setCellStyle(bodyStyle);
                }
            }

            String encodedName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setHeader("Content-Disposition",
                    "attachment; filename=\"" + encodedName + ".xlsx\"; filename*=UTF-8''" + encodedName + ".xlsx");
            response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
            try (OutputStream out = response.getOutputStream()) {
                workbook.write(out);
                out.flush();
            }
        } catch (Exception e) {
            throw new com.mall.common.BusinessException("导出 Excel 失败：" + e.getMessage());
        }
    }

    /**
     * 导出空白模板
     */
    public static void exportTemplate(HttpServletResponse response, String fileName, String sheetName,
                                      String[] headers, String[] tips) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(sheetName);
            CellStyle headerStyle = buildHeaderStyle(workbook);
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, Math.min(40, Math.max(14, headers[i].length() * 4 + 8)) * 256);
            }
            if (tips != null && tips.length > 0) {
                Sheet tipSheet = workbook.createSheet("填写说明");
                for (int i = 0; i < tips.length; i++) {
                    Row row = tipSheet.createRow(i);
                    row.createCell(0).setCellValue(tips[i]);
                }
                tipSheet.setColumnWidth(0, 100 * 256);
            }
            String encodedName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition",
                    "attachment; filename=\"" + encodedName + ".xlsx\"; filename*=UTF-8''" + encodedName + ".xlsx");
            try (OutputStream out = response.getOutputStream()) {
                workbook.write(out);
                out.flush();
            }
        } catch (Exception e) {
            throw new com.mall.common.BusinessException("导出模板失败：" + e.getMessage());
        }
    }

    private static CellStyle buildHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        return style;
    }

    private static CellStyle buildBodyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.HAIR);
        style.setBorderRight(BorderStyle.HAIR);
        return style;
    }

    /**
     * 读取单元格文本，兼容数字/公式/日期等类型
     */
    public static String readCell(Cell cell) {
        if (cell == null) {
            return null;
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                double d = cell.getNumericCellValue();
                if (d == Math.floor(d) && !Double.isInfinite(d)) {
                    yield String.valueOf((long) d);
                }
                yield String.valueOf(d);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> null;
        };
    }
}
