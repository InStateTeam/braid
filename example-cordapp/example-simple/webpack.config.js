// webpack.config.js
module.exports = {
  entry: './app.js',
  output: {
    filename: './dist/bundle.js',
    library: 'app',
    libraryTarget: 'var'
  },
  devtool: "source-map",
  module: {
    rules: [
      {
        test: /\.js$/,
        exclude: /node_modules/,
        use: {
          loader: 'babel-loader',
          options: {
            presets: ['env']
          }
        }
      }
    ]
  }
};
