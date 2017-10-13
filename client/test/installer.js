require("babel-register")({
    presets: ["es2015"],
    plugins: ["transform-async-to-generator", "transform-regenerator"],
    sourceMaps: true,
    retainLines: true
});
require('./app.js')