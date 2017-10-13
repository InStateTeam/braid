import 'babel-register'
import 'babel-polyfill'
import RPCProxy from '../dist/index.js'

export default async function App () {
  const calculator = await RPCProxy('ws://localhost:8088/api/calculator')
  console.log(await calculator.add(1, 2))
}
