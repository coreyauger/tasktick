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
	addProject(p: Project) {        
        this.projects = [...this.projects.filter(x => x.id != p.id), p];
    }
    addTask(t: Task){
        const proj = this.projects.find( x => x.id == t.project)
        if(proj){
            proj.tasks = [...proj.tasks.filter(x => x != t.id), t.id]
        }
    }
} 

class TaskStore{
    @observable tasks: { [id:string]:Task } = {};    
    addTask(t: Task) {
        this.tasks[t.id] = t
    }
    addNote(n: Note){
        const t = this.tasks[n.task]
        if(t){
            t.notes = [...t.notes.filter(x => x.id != n.id), n]
        }
    }
}


const socketStore = new SocketStore();
const taskStore = new TaskStore();
const projectStore = new ProjectStore();
const userStore = new UserStore();

const stores = {
    routing: routingStore,
    socketStore,
    taskStore,
    projectStore,    
    userStore,
    clear(){
        taskStore.tasks = {}
        projectStore.projects = []
    }
} 

export default stores;