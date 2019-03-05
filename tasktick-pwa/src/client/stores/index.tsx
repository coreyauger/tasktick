import { RouterStore } from 'mobx-react-router';
import { Note, Project, Task, User} from './data'
import { observable, computed } from 'mobx';
import { TasktickSocket } from '../socket/WebSocket';


const routingStore = new RouterStore();

class SocketStore{
    socket: TasktickSocket;

    connect(tb: TasktickSocket){
        this.socket = tb;
    }     
}

class UserStore {
	@observable users: User[] = [];    
    constructor() {}
	addUser(e: User) {
		this.users.push(e);
	}
}

class ProjectStore {
	@observable projects: Project[] = [];    
    constructor() {}
	addProject(e: Project) {
		this.projects.push(e);
	}
} 

class TaskStore{
    @observable tasks: { [id:string]:Task } = {};    
    addTask(t: Task) {
        this.tasks[t.id] = t
    }
}

class NoteStore{
    @observable notes: { [id:string]:Note } = {};    
    addTask(n: Note) {
        this.notes[n.id] = n
    }
}


const socketStore = new SocketStore();
const taskStore = new TaskStore();
const projectStore = new ProjectStore();
const noteStore = new NoteStore();
const userStore = new UserStore();

const stores = {
    routing: routingStore,
    socketStore,
    taskStore,
    projectStore,
    noteStore,    
    userStore
} 

export default stores;