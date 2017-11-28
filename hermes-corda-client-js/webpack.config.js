// webpack.config.js
module.exports = {
  entry: {
    'hermes-corda-client': __dirname + '/hermes-corda-proxy.js'
  },
  output: {
    path: __dirname + '/dist',
    filename: 'hermes-corda-client.js',
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
