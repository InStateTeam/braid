import React from 'react';

function TodoItem(props) {

  var checked;

  return(
    <li className='todo-item-component'>
      <input
        type="checkbox" 
        ref={function(node) {
              checked = node;              
            }} 
        onChange={function(e){ props.complete(props.todo.id); }} />
        <span>
          {props.todo.text}
        </span>
        <button 
          onClick={function(e){props.remove(props.todo.id)}} >
          &times;
        </button>
    </li>
  );
}

export default TodoItem;