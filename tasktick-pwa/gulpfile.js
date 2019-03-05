var gulp = require('gulp');
var ts = require('gulp-typescript');
var webpack = require('webpack');
var gutil = require('gulp-util');
var nodemon = require('nodemon');

/**
 * Launch server and hot reload on changes.
 */
gulp.task('start', ["build:server", "build:client"], cb => {
    nodemon({
        watch: ["build"],
        script: "build/server/main.js",
        ignore: ["src", "node_modules"]
    })
    nodemon.once("start", cb);
})

gulp.task('watch', [], () => {
    nodemon({
        watch: ["build"],
        script: "build/server/main.js",
        ignore: ["src", "node_modules"]
    })
    var config = [
        webpackConfig("./src/client/index", "bundle.js")];     
    config.forEach(function(x) { x.watch = true });
    webpack(config, (err, stats) => {
        if(err) {
            throw new gutil.PluginError("webpack:build", err);
        }
        else {
            gutil.log("[webpack]", stats.toString({}));
        }
    })    
})

gulp.task('start-production', ["build:server", "production"], cb => {
    nodemon({
        watch: ["build"],
        script: "build/server/main.js",
        ignore: ["src", "node_modules"]
    })
    nodemon.once("start", cb);
})

gulp.task('watch-production', ["start-production"], () => {
    gulp.watch(["src/client/**"], ["production"]);
    gulp.watch(["src/server/**"], ["build:server"]);
    gulp.watch(["src/shared/**"], ["production", "build:server"]);
});

var tsProject = ts.createProject('tsconfig.json');

function webpackConfig(entry, out) {
    return {
        entry: entry,
        devtool: "source-map",
        stats: {
            errorDetails: true
        },
        node: {
            fs: "empty"
          },
        module: {
            loaders: [
                {
                    test: /\.tsx?$/,
                    loader: 'ts-loader'
                },
                {
                    test: /\.less$/,
                    loaders: ["style", "css", "less"]
                },              
                {
                    test: /(\.png|\.jpg)$/,
                    loader: "url-loader"
                },
                {
                    test: /\.(glsl|frag|vert)$/,
                    loader: __dirname+'/glsl-loader'
                }
            ]
        },
        resolve: {
            extensions: [".ts", ".js", ".tsx", ""]
        },
        output: {
            path: "www/",
            filename: out
        },
        plugins: [
            new webpack.DefinePlugin({
                'process.env': {
                    'DEBUG': true
                }
            }),
            new webpack.optimize.DedupePlugin()
        ]
    }
}

function webpackProductionConfig(entry, out) {
    var entry = webpackConfig(entry, out);
    entry.plugins = [
        new webpack.DefinePlugin({
            'process.env': {
                'NODE_ENV': JSON.stringify('production'),
                'DEBUG': false
            }
        }),
        new webpack.optimize.DedupePlugin(),
        new webpack.optimize.UglifyJsPlugin({ mangleProperties: true }),
    ]
    return entry;
}

gulp.task('build:server', () => {
    gulp.src('src/**/*.ts')
        .pipe(tsProject())
        .pipe(gulp.dest('build'));
})

gulp.task('build:client', cb => {
    var config = webpackConfig("./src/client/index", "bundle.js");

    webpack(config, (err, stats) => {
        if(err) {
            throw new gutil.PluginError("webpack:build", err);
        }
        else {
            gutil.log("[webpack]", stats.toString({}));
            cb();
        }
    })
})

gulp.task('production', cb => {
    var config = webpackProductionConfig("./src/client/index.tsx", "bundle.js")

    webpack(config, (err, stats) => {
        if(err) {
            throw new gutil.PluginError("webpack:build", err);
        }
        else {
            gutil.log("[webpack]", stats.toString({}));
            cb();
        }
    })
})

gulp.task('default', ["build:server", "build:client"]);

