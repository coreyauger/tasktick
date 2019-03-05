
// data time format "yyyy-MM-dd'T'HH:mm:ss.SSS" Z with tz
export const toDateTime = (date: Date): string => 
    date.getUTCFullYear() + "-" + (date.getUTCMonth()+1) + "-" + date.getUTCDate() + "T" + date.getUTCHours() + ":" + date.getUTCMinutes() + ":" + date.getUTCSeconds() + "." + date.getMilliseconds() + "Z" 


export type uuid = string


export interface SocketEvent{
    payload: any;
} 

export interface User{
    id: uuid, 
    firstName: string
    lastName: string
    email: string
}

export interface Note{
    id: uuid, 
    user: uuid, 
    note: string, 
    date: number
}

export interface Task{
    id: uuid;
    name: string;
    description: string,
    done: boolean,
    assigned?: uuid,
    startDate?: number,
    endDate?: number,
    lastUpdated: number,
    section: string,
    parent?: uuid,
    notes: uuid[]
}

export interface Project{
    id: uuid,
    name: string,
    owner: uuid,
    team: uuid,
    description: string,
    imgUrl?: string,
    tasks: uuid[]
}

export interface ServiceException{
    message: string;
    stackTrace: string[];
    extra: any
}

export const uuidv4 = () => {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
      var r = Math.random() * 16 | 0, v = c == 'x' ? r : (r & 0x3 | 0x8);
      return v.toString(16);
    });
  }

// TODO: CA - we want a way to fetch these schema at runtime.
export const Serializers = {    
 //   serviceExceptionType: avro.Type.forSchema({"type":"record","name":"ServiceException","namespace":"io.surfkit.typebus.event","fields":[{"name":"message","type":"string"},{"name":"stackTrace","type":{"type":"array","items":"string"}},{"name":"extra","type":{"type":"map","values":"string"},"default":{}}]}),    
}

export const SerializerMappings = {
    //"io.surfkit.typebus.event.package.ServiceException": Serializers.serviceExceptionType,    
}
