import React from 'react';

function TodoForm(props) {

  var input;

  function handleEnter(e, v){
    console.log(v);
    props.addTodo(v);
  }

  return (
    <div className='todo-form-component'>
      <input
        type='text'
        ref={function (node){
          input = node;
        }} 
        onKeyUp={
          function(e){
            if(e.keyCode == 13){
              var v = input.value;
              input.value = '';
              handleEnter(e, v);             
            }
          }
        }/>
    </div>
  );
}
export default TodoForm;
