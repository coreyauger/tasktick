import * as React from 'react';
import {observer} from 'mobx-react';
import { Theme } from '@material-ui/core/styles/createMuiTheme';
import createStyles from '@material-ui/core/styles/createStyles';
import withStyles, { WithStyles } from '@material-ui/core/styles/withStyles';
import Grid from '@material-ui/core/Grid';
import withRoot from '../withRoot';
import ProjectCard from '../components/ProjectCard';
import { Task, Project, uuidv4 } from '../stores/data';
import TaskCard from '../components/TaskCard';
import { Dialog, DialogTitle, DialogContent, DialogContentText, TextField, DialogActions, Button } from '@material-ui/core';
import AddIcon from '@material-ui/icons/Add';
import { Fab } from '@material-ui/core';

const styles = (theme: Theme) =>
  createStyles({
    root: {
      
    },
    tableContainer: {
      height: 320,
    },
    paper: {
      padding: theme.spacing.unit * 2,
      textAlign: 'center',
      color: theme.palette.text.secondary,
    },
    fab: {
      margin: theme.spacing.unit,
      position: "absolute",
      right: 0,
      bottom: 0,
    },
  });

type State = {
  task: Task;    
  name: string;
  description: string;
  showAddProject: boolean;
};

interface Props {
  store: any;  
  project?: string
};

@observer
class Projects extends React.Component<Props & WithStyles<typeof styles>, State> {
  state = {
    task: undefined,    
    name: "",
    description: "",
    showAddProject: false
  };
 
  onTaskSelect =  (task: Task) => {
    this.setState({...this.state, task});
  }
  clearTask = () => {
    this.setState({...this.state, task: undefined});
  }
  onProjectSelect = (project: Project) => {
    const { location, push, goBack } = this.props.store.routing; // CA - inject above did not work.. you should see this as a "prop" (investigate)
    this.setState({task: undefined})
    push('/p/project/'+project.id)
  }   
  showAddProject = () =>{
    this.setState({showAddProject:true})
  }
  updateProjectName = (event) => {
    this.setState({ name: event.target.value });
  };
  updateProjectDescription = (event) => {
    this.setState({ description: event.target.value });
  };
  hideAddProject = () =>{
    this.setState({showAddProject:false, name: "", description: ""})
  }
  saveProject = () => {
    // this is a hack right now to send 'uuidv4()' so that we have a value to serialize to a UUID (note owner is replaced service side anyways)
    this.props.store.socketStore.socket.send("NewProject", {name: this.state.name, description: this.state.description, owner: uuidv4(), team: uuidv4()})  
    this.hideAddProject();
  }

  render() {    
    const { location, push, goBack } = this.props.store.routing; // CA - inject above did not work.. you should see this as a "prop" (investigate)
    const pathname = location.pathname.split('/')
    const selectedProject = this.props.project ? this.props.project : (pathname.length == 4) ? pathname[3] : undefined;        
    const projectList = this.props.store.projectStore.projects
    const selectProject = this.props.store.projectStore.projects.find(x => x.id == selectedProject)
    if(this.state.task && selectProject.id != this.state.task.project){
      this.setState({task: undefined})
    }
    const  classes = this.props.classes;
    return (<div className={this.props.classes.root}>
      {selectProject ? (
         <Grid container spacing={24}> 
            <Grid item xs={4} key={"sel"+selectProject.id}><ProjectCard store={this.props.store} project={selectProject} onTaskSelect={this.onTaskSelect} expanded={true} /></Grid>
            <Grid item xs={8} key="taskStream">

              {this.state.task ? 
                <TaskCard store={this.props.store} task={this.state.task} />
              :null}
            </Grid>
         </Grid>
      ):(
      <Grid container spacing={24}>
          {projectList.map( (x: Project) => <Grid item xs={4} key={"_pro_"+x.id}><ProjectCard project={x} store={this.props.store} onTaskSelect={this.onTaskSelect} onProjectSelect={this.onProjectSelect} /></Grid> )}
      </Grid>)}

      <Fab color="primary" aria-label="Add" className={classes.fab} onClick={this.showAddProject} >
        <AddIcon />
      </Fab>
      <Dialog
          open={this.state.showAddProject}
          onClose={this.hideAddProject}
          aria-labelledby="form-dialog-title"
        >
          <DialogTitle id="form-dialog-title">New Project</DialogTitle>
          <DialogContent>
            <DialogContentText>
              Create a new project.
            </DialogContentText>
            <TextField onChange={this.updateProjectName} autoFocus margin="dense" id="name" label="Project Name" type="text" fullWidth  />
            <TextField onChange={this.updateProjectDescription} margin="dense" id="description" label="Project Description" type="text" fullWidth />
          </DialogContent>
          <DialogActions>
            <Button onClick={this.hideAddProject} color="primary">Cancel</Button>
            <Button onClick={this.saveProject} disabled={this.state.name == ""} color="primary">Save</Button>
          </DialogActions>
        </Dialog>
      </div>);       
  }
}

export default withRoot(withStyles(styles)(Projects));