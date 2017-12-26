// webpack.config.js
module.exports = {
  entry: {
    'braid-corda-client': __dirname + '/braid-corda-proxy.js'
  },
  output: {
    path: __dirname + '/dist',
    filename: 'braid-corda-client.js',
    libraryTarget: 'umd',
    umdNamedDefine: true
  },
  devtool: 'eval',
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
