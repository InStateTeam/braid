// webpack.config.js
module.exports = {
  entry: {
    'hermes-client': __dirname + '/hermes-service-proxy.js'
  },
  output: {
    path: __dirname + '/dist',
    filename: 'hermes-client.js',
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
