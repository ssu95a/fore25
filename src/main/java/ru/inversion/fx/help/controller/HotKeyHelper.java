/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.help.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ResourceBundle;
import java.util.Scanner;
import ru.inversion.utils.S;

/**
 *
 * @author antonovdi
 */
public class HotKeyHelper {

    public Logger logger = LoggerFactory.getLogger(getClass().getName());
    public static ResourceBundle bundle = ResourceBundle.getBundle("ru/inversion/fx/help/hotkey/keys");
    private static String xml;

    public String getKeysXml() throws IOException, URISyntaxException {
        if ( S.isNullOrEmpty( xml ) ) {
            try (InputStream is = getClass().getClassLoader().getResourceAsStream("ru/inversion/fx/help/hotkey/keys.xml");
                 Scanner scanner = new Scanner(is, "UTF-8").useDelimiter("\\A")) {
                xml = scanner.hasNext() ? scanner.next() : "";
            }
        }
        return xml;
    }

    public String getLocalizeName(String key) {
        return bundle.getString(key);
    }

//       public static void main(String[] args) throws IOException, URISyntaxException {
//
//            System.out.println(new HotKeyHelper().getLocalizeName("GROUP_TABLE"));
//       }
}
