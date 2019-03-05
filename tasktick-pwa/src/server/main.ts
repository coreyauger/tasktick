import * as express from 'express';
import * as compression from 'compression';

let app = express();
app.use(function(req, res, next) {
    console.log(req.url);
    next();
})
app.use(compression());
app.get("/t/**", (req,res) => {
    res.sendFile("index.html", { root: __dirname+"/../../www"})
});
app.get("/invite/**", (req,res) => {
    res.sendFile("index.html", { root: __dirname+"/../../www"})
});
app.get("/dev/**", (req,res) => {
    res.sendFile("developer.html", { root: __dirname+"/../../www"})
});
app.use(express.static(__dirname+"/../../www"));

app.listen(3001);