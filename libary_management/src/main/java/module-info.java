@SuppressWarnings("all")
module com.library { 
    requires javafx.controls;
    requires javafx.fxml;
    requires transitive javafx.graphics; 
    
    requires java.sql;
    requires com.zaxxer.hikari;
    requires org.slf4j;
    requires jbcrypt;

    opens com.library to javafx.fxml; 
    exports com.library;

    opens com.library.controller to javafx.fxml; 
    exports com.library.controller;
    
    opens com.library.model to javafx.base;
    exports com.library.model;
}