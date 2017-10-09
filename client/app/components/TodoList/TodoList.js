import React from 'react';
import TodoItem from 'components/TodoItem/TodoItem';

function TodoList(props) {
  var todoNode = props.todos.map(function(todo, index) {
    return(
      <TodoItem 
        todo={todo}
        key={todo.id}
        complete={props.complete}
        remove={props.remove}
        index={index + 1} />
     );
  });

  return(
    <ul className='todo-list-component'>
      {todoNode}
    </ul>
  )
}

export default TodoList;
