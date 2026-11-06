package ru.inversion.icons.testgui;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class TestIconMain extends Application {
    public static void main( String[] args ) {
        System.setProperty( "jinv.icon.path", "X:\\B21\\ICONS" );
        launch( args );
    }

    @Override
    public void start( Stage primaryStage ) throws Exception {
        Parent root = FXMLLoader.load( getClass().getResource( "/fxml/sample.fxml" ) );
        primaryStage.setTitle( "InvIconService" );
        Scene primaryScene = new Scene( root, 320, 320 );
        primaryScene.getStylesheets().add( getClass().getResource( "/style/style.css" ).toExternalForm() );
        primaryStage.setScene( primaryScene );
        primaryStage.show();
    }
}
