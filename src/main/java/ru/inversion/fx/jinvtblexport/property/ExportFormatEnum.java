/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.jinvtblexport.property;

/**
 *
 * @author polyatykina
 */
public enum ExportFormatEnum {
    
      Pdf("pdf")
    , Xps("xps")
    , Rtf("rtf")
    , Text("txt")
    , Excel("xls")
    , ExcelXml("xml")
    , Excel2007("xlsx")
    , Word("doc")
    , Word2007("docx")
    , Xml("xml")
    , Csv("csv")
    , Sylk("slk")
    , ImageBmp("bmp")
    , ImagePng("png")
    , ImageJpeg("jpg")
    , ImagePcx("pcx")
    , Html("html")
    , ImageSvg("svg")
    , ImageSvgz("svgz")
    , Mdc("mdc");

    ExportFormatEnum(String fileExtention) {
        this.fileExtention = fileExtention;
    }

    public String getFileExtention() {return fileExtention;}

    private String fileExtention;

}
