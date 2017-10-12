'use strict';

var App = function () {
  var _ref = _asyncToGenerator( /*#__PURE__*/regeneratorRuntime.mark(function _callee() {
    var calculator, accounts;
    return regeneratorRuntime.wrap(function _callee$(_context) {
      while (1) {
        switch (_context.prev = _context.next) {
          case 0:
            _context.next = 2;
            return (0, _index2.default)('ws://localhost:8080/api/calculator');

          case 2:
            calculator = _context.sent;
            _context.t0 = console;
            _context.next = 6;
            return calculator.add(1, 2);

          case 6:
            _context.t1 = _context.sent;

            _context.t0.log.call(_context.t0, _context.t1);

            _context.t2 = console;
            _context.next = 11;
            return calculator.subtract(1, 2);

          case 11:
            _context.t3 = _context.sent;

            _context.t2.log.call(_context.t2, _context.t3);

            _context.t4 = console;
            _context.next = 16;
            return calculator.multiply(1, 2);

          case 16:
            _context.t5 = _context.sent;

            _context.t4.log.call(_context.t4, _context.t5);

            _context.t6 = console;
            _context.next = 21;
            return calculator.divide(10, 2);

          case 21:
            _context.t7 = _context.sent;

            _context.t6.log.call(_context.t6, _context.t7);

            _context.t8 = console;
            _context.next = 26;
            return calculator.exp(2, 3);

          case 26:
            _context.t9 = _context.sent;

            _context.t8.log.call(_context.t8, _context.t9);

            _context.next = 30;
            return (0, _index2.default)('ws://localhost:8080/api/accounts');

          case 30:
            accounts = _context.sent;
            _context.t10 = console;
            _context.next = 34;
            return accounts.createAccount("fred");

          case 34:
            _context.t11 = _context.sent;

            _context.t10.log.call(_context.t10, _context.t11);

            _context.t12 = console;
            _context.next = 39;
            return accounts.createAccount("jim");

          case 39:
            _context.t13 = _context.sent;

            _context.t12.log.call(_context.t12, _context.t13);

            _context.t14 = console;
            _context.next = 44;
            return accounts.getAccounts();

          case 44:
            _context.t15 = _context.sent;

            _context.t14.log.call(_context.t14, _context.t15);

            _context.t16 = console;
            _context.next = 49;
            return accounts.updateAccount({ id: "1", name: "henry" });

          case 49:
            _context.t17 = _context.sent;

            _context.t16.log.call(_context.t16, _context.t17);

            _context.t18 = console;
            _context.next = 54;
            return accounts.getAccounts();

          case 54:
            _context.t19 = _context.sent;

            _context.t18.log.call(_context.t18, _context.t19);

          case 56:
          case 'end':
            return _context.stop();
        }
      }
    }, _callee, this);
  }));

  return function App() {
    return _ref.apply(this, arguments);
  };
}();

// export default App;

var _index = require('../dist/index.js');

var _index2 = _interopRequireDefault(_index);

function _interopRequireDefault(obj) {
  return obj && obj.__esModule ? obj : { default: obj };
}

function _asyncToGenerator(fn) {
  return function () {
    var gen = fn.apply(this, arguments);return new Promise(function (resolve, reject) {
      function step(key, arg) {
        try {
          var info = gen[key](arg);var value = info.value;
        } catch (error) {
          reject(error);return;
        }if (info.done) {
          resolve(value);
        } else {
          return Promise.resolve(value).then(function (value) {
            step("next", value);
          }, function (err) {
            step("throw", err);
          });
        }
      }return step("next");
    });
  };
}

document.addEventListener('DOMContentLoaded', function () {
  // do your setup here
  App();
  console.log('Initialized app');
});