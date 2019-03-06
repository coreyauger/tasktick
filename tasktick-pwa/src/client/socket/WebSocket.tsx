import {SocketEvent, toDateTime, Project, User, Task, Note} from '../stores/data';
import stores from '../stores'

const WS_OPEN = 1

const typeMap = {    
    "io.surfkit.gateway.api.ProjectList" : (e: SocketEvent) =>{        
        e.payload.projects.forEach( x => {
            const project: Project = {
                id: x.id,
                name: x.name,
                owner: x.owner,
                team: x.team,
                description: x.description,
                imgUrl: x.imgUrl,
                tasks: x.tasks.map(y => y.id)
            }
            stores.projectStore.addProject(project) 
            x.tasks.forEach(y => {
                stores.taskStore.addTask(y as Task)      
                stores.projectStore.addTask(y as Task)    
            })
        })      
    },    
    "io.surfkit.gateway.api.ProjectRefList" : (e: SocketEvent) =>{        
        e.payload.projects.forEach( x => {
            const project: Project = {
                id: x.id,
                name: x.name,
                owner: "",
                team: "",
                description: "",                
                tasks: []
            }
            stores.projectStore.addProject(project)           
        })
    },
    "io.surfkit.gateway.api.UserList" : (e: SocketEvent) =>{        
        e.payload.users.forEach( x => {            
            stores.userStore.addUser(x as User)           
        })
    },
    "io.surfkit.gateway.api.TaskList" : (e: SocketEvent) =>{        
        e.payload.tasks.forEach( x => {            
            stores.taskStore.addTask(x as Task)      
            stores.projectStore.addTask(x as Task)     
        })
    },
    "io.surfkit.gateway.api.NoteList" : (e: SocketEvent) =>{        
        e.payload.notes.forEach( x => {            
            stores.taskStore.addNote(x as Note)   
        })
    },
}

/*
RECEIVED: {"payload":{"msg":"pong","_type":"io.surfkit.gateway.api.Test"}}
*/

export class TasktickSocket{

    ws: WebSocket = null
    cancelable: any = null
    queue = []
    
    constructor(token: string){
        console.log("RARG !!")
        this.ws = new WebSocket("ws://localhost:9000/ws/stream/"+token)
        this.ws.onopen =  (event) => {
            console.log("WEBSOCKET IS CONNECTED3 !!!")            
            const getUser = JSON.stringify({payload:{ts: new Date().getTime(), _type: 'io.surfkit.gateway.api.GetUser'}}) 
            console.log("sending getUser", getUser)
            this.ws.send(getUser)
            const GetProjects = JSON.stringify({payload:{skip: 0, take: 50, _type: 'io.surfkit.gateway.api.GetProjects'}}) 
            console.log("sending GetProjects", GetProjects)
            this.ws.send(GetProjects)            
            while(this.queue.length != 0){
                const data = this.queue.shift()
                this.send(data.eventType, data.payload)
            }
        }
        this.ws.onmessage = async (event) => {
            console.log("got a WS message", event)
            const socketEvent = JSON.parse(event.data) as SocketEvent
            console.log("ws socketEvent["+socketEvent.payload._type+"]", socketEvent)
            if(typeMap[socketEvent.payload._type]){
                typeMap[socketEvent.payload._type](socketEvent)
            }                   
        }   
        this.ws.onclose = (event) => {
            console.error("WebSocket is closed now. !!!!!!!!!!!!!!!!!!!");
            if(this.cancelable){
                clearInterval(this.cancelable);
                this.cancelable = null;
            }
        };   
        this.cancelable = setInterval(() =>{
            console.log("lub dub..")
            this.ws.send(JSON.stringify( {payload:{ts: new Date().getTime(), _type: 'io.surfkit.gateway.api.HeartBeat'}} ) )
        }, 25 * 1000) // every 25 seconds send a heart-beat to keep alive
    }

    send(eventType: string, payload: any){
        if(this.ws.readyState != WS_OPEN){
            this.queue.push({eventType, payload})
        }else{
            const socketEvent = this.mkSocketEvent(eventType, payload)
            console.log("SEND EVENT: " + eventType, socketEvent)
            this.ws.send(JSON.stringify(socketEvent))
        }
    }
    close = () =>{
        if(this.ws)
            this.ws.close();
    }

    mkSocketEvent = (eventType: string, payload: any) => (
        {            
            payload: {...payload, _type: "io.surfkit.gateway.api."+eventType}
        } as SocketEvent
    )
}






