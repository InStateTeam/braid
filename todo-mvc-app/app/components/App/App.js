import React from 'react';
import TodoForm from 'components/TodoForm/TodoForm';
import TodoList from 'components/TodoList/TodoList';
const count = 0;

export default class App extends React.Component {
  constructor() {
    super();
    this.state = {
      data: []
    }
    this.count = 0;
    this.addTodo = this.addTodo.bind(this);
    this.handleRemove = this.handleRemove.bind(this);
    this.handleComplete = this.handleComplete.bind(this);
  }

  addTodo(val){    
    var todo = {
      text: val,
      id: this.count++,
      active: true
    }
    var data = this.state.data;
    data.push(todo);

    this.setState({data: data});   
  }

  handleRemove(id){
    var remainder = this.state.data.filter(function(todo){
      if(todo.id !== id) return todo;
    });

    this.setState({data: remainder})
  }

  handleComplete(id){
    var data = this.state.data;
    for(var i in data){
      if(data[i].id == id){
        data[i].active = !data[i].active;
        break;
      }
    }
    this.setState({data: data});
  }

  render() {
    return (
      <div className='app-component'>
        <h1>todos</h1>
        <TodoForm addTodo={this.addTodo} />
        <TodoList 
          todos={this.state.data}
          complete={this.handleComplete}
          remove={this.handleRemove}
        />
      </div>
    );
  }
}
