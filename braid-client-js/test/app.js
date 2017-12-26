import 'babel-polyfill'
import App from './client'
import startServer from './server'

startServer();
App().then(() => {
    console.log("done");
}).catch(err => {
    console.error(err);
});
