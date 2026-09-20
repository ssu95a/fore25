package ru.inversion.fx.dialog;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import ru.inversion.fx.form.ViewContext;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.Map;
import java.util.ResourceBundle;

public class JInvAboutDialog {
    
    private final Stage stage;
    private  Label labelApplicationName; // Наименование приложения
    private  Label labelReleaseVersion; // Версия выпуска
    private  Label labelReleaseDate; // Дата выпуска

    private  Label labelCopyright = new Label("Copyright \u00a9 Inversion"); // Авторское право
    
    private ResourceBundle bundle;
    
    public static final String APPLICATION_NAME = "APPLICATION_NAME";
    public static final String RELEASE_VERSION = "RELEASE_VERSION";
    public static final String RELEASE_DATE = "RELEASE_DATE";

    
    public JInvAboutDialog() {
        this(Collections.EMPTY_MAP,null);       
    }

    public JInvAboutDialog(Map<String,String> param, ViewContext vc) {
        this.bundle = ResourceBundle.getBundle("fore");
        stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.getIcons().add(new Image("img/inv.png"));
        
        VBox root = new VBox();
        root.setAlignment(Pos.TOP_CENTER);
        root.setStyle("-fx-background-color: #fdfeff");
        root.setFillWidth(true);
        
        
        HBox imgBox = new HBox();
        imgBox.setPadding(new Insets(10, 10, 10, 10));
        imgBox.setSpacing(15);
        imgBox.setAlignment(Pos.CENTER);
        
        VBox.setVgrow(imgBox, Priority.ALWAYS);
        
        Image image = new Image("img/inv.png");
        
        ImageView iv = new ImageView();
        iv.setImage(image);
        iv.setFitWidth(150);
        iv.setPreserveRatio(true);
        iv.setSmooth(true);
        iv.setCache(true);
        
        HBox.setMargin(iv, new Insets(15, 15, 15, 15));
        
        imgBox.getChildren().add(iv);    
        
        
        VBox vbox = new VBox();       
     
        vbox.setAlignment(Pos.TOP_LEFT);
        vbox.setSpacing(5);
        vbox.setFillWidth(true);
        
        if (param != null){
            if (param.containsKey(APPLICATION_NAME)){
                labelApplicationName = new Label(param.get(APPLICATION_NAME));
                labelApplicationName.setStyle("-fx-font-size: 24; -fx-font-weight: bold; -fx-text-fill: #373942;");
                setTitle(param.get(APPLICATION_NAME));
                vbox.getChildren().add(labelApplicationName);
            }
            
            if (param.containsKey(RELEASE_VERSION)){
                labelReleaseVersion = new Label(MessageFormat.format(bundle.getString("JINVABOUT_DIALOG.RELEASE_VERSION"), new Object[] { param.get(RELEASE_VERSION) })); 
                labelReleaseVersion.setStyle("-fx-font-size: 18;");
                vbox.getChildren().add(labelReleaseVersion);
            }
            
            if (param.containsKey(RELEASE_DATE)){
                labelReleaseDate = new Label(MessageFormat.format(bundle.getString("JINVABOUT_DIALOG.RELEASE_DATE"), new Object[] { param.get(RELEASE_DATE) })); 
                labelReleaseDate.setStyle("-fx-font-size: 14; -fx-text-fill: #444;");
                vbox.getChildren().add(labelReleaseDate);
            }
                        
        }
        
        
        HBox.setHgrow(vbox, Priority.ALWAYS);
        
        imgBox.getChildren().add(vbox);
        
        HBox boxCopyright = new HBox(labelCopyright);
        boxCopyright.setAlignment(Pos.CENTER);
        boxCopyright.setStyle("-fx-background-color: #eeeeef;");
        
        boxCopyright.setMinHeight(70);
               
        
        root.getChildren().addAll(imgBox, boxCopyright);
        Scene lScene = new Scene(root, 600, 280);
        stage.setScene(lScene);        
        
    }

    private void setTitle(String applicationName) {
        stage.setTitle(applicationName);
    }
    
    public void show(){
        this.stage.show();
    }

    public void setApplicationName(String applicationName) {
        this.labelApplicationName.setText(applicationName);
        setTitle( applicationName);
    }

    public void setReleaseVersion(String releaseVersion) {
        this.labelReleaseVersion.setText(releaseVersion);
    }

    public void setReleaseDate(String releaseDate) {
        this.labelReleaseDate.setText(releaseDate);
    }

    public void setCopyright(String copyright) {
        this.labelCopyright.setText(copyright);
    }    
}