"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var express = require("express");
var compression = require("compression");
var app = express();
app.use(function (req, res, next) {
    console.log(req.url);
    next();
});
app.use(compression());
app.get("/t/**", function (req, res) {
    res.sendFile("index.html", { root: __dirname + "/../../www" });
});
app.get("/invite/**", function (req, res) {
    res.sendFile("index.html", { root: __dirname + "/../../www" });
});
app.get("/dev/**", function (req, res) {
    res.sendFile("developer.html", { root: __dirname + "/../../www" });
});
app.use(express.static(__dirname + "/../../www"));
app.listen(3001);
