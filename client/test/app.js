import RPCProxy from '../dist/index.js'

async function App () {
  const calculator = await RPCProxy('ws://localhost:8080/api/calculator')
  console.log(await calculator.add(1, 2))
  console.log(await calculator.subtract(1, 2))
  console.log(await calculator.multiply(1, 2))
  console.log(await calculator.divide(10, 2))
  console.log(await calculator.exp(2, 3))

  const accounts = await RPCProxy('ws://localhost:8080/api/accounts')
  console.log(await accounts.createAccount("fred"))
  console.log(await accounts.createAccount("jim"))
  console.log(await accounts.getAccounts())
  console.log(await accounts.updateAccount({ id: "1", name: "henry"}))
  console.log(await accounts.getAccounts())
}

// export default App;

document.addEventListener('DOMContentLoaded', () => {
  // do your setup here
  App()
  console.log('Initialized app');
});


