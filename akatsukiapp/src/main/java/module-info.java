module com.akatsuki {
    requires javafx.controls;
    requires javafx.media;
    requires java.sql;
    requires java.net.http;
    requires com.google.gson;

    opens com.akatsuki to javafx.graphics;
    opens com.akatsuki.model to com.google.gson;

    exports com.akatsuki;
    exports com.akatsuki.model;
    exports com.akatsuki.database;
    exports com.akatsuki.service;
    exports com.akatsuki.ui;
}
