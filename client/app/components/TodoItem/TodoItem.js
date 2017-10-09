import React from 'react';

function TodoItem(props) {

  var checked;

  return(
    <li className='todo-item-component'>
      <div className="tick">
        <input
          id="done"
          type="checkbox" 
          ref={function(node) {
                checked = node;              
              }} 
          onChange={function(e){ props.complete(props.todo.id); }} />
        <label htmlFor="done"></label> 
       </div>
        <span className="index">{props.index}.</span>
        <span>
          {props.todo.text}
        </span>
        <button 
          onClick={function(e){props.remove(props.todo.id)}} >
          <img src="svg/cross.svg" alt="cross" />
        </button>
    </li>
  );
}

export default TodoItem;