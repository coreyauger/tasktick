import * as React from 'react';
import {observer} from 'mobx-react';
import { Theme } from '@material-ui/core/styles/createMuiTheme';
import createStyles from '@material-ui/core/styles/createStyles';
import withStyles, { WithStyles } from '@material-ui/core/styles/withStyles';
import Grid from '@material-ui/core/Grid';
import withRoot from '../withRoot';
import { Dialog } from '@material-ui/core';
import ProjectCard from '../components/ProjectCard';
import { Task, Project } from '../stores/data';

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
  });

type State = {
  task: Task;    
};

interface Props {
  store: any;  
  project?: string
};

@observer
class Projects extends React.Component<Props & WithStyles<typeof styles>, State> {
  state = {
    task: undefined,    
  };
 
  onTaskSelect =  (task: Task) => {
    this.setState({...this.state, task});
  }
  clearTask = () => {
    this.setState({...this.state, task: undefined});
  }
  onProjectSelect = (project: Project) => {
    const { location, push, goBack } = this.props.store.routing; // CA - inject above did not work.. you should see this as a "prop" (investigate)
    push('/p/project/'+project.id)
  }   

  render() {    
    const { location, push, goBack } = this.props.store.routing; // CA - inject above did not work.. you should see this as a "prop" (investigate)
    const pathname = location.pathname.split('/')
    const selectedProject = this.props.project ? this.props.project : (pathname.length == 4) ? pathname[3] : undefined;
    console.log("project", this.props.project)
    
    const projectList = this.props.store.projectStore.projects
    const selectProject = this.props.store.projectStore.projects.find(x => x.id == selectedProject)
    console.log(selectProject)
    const  classes = this.props.classes;
    return (<div className={this.props.classes.root}>
      {selectProject ? (
         <Grid container spacing={24}> 
            <Grid item xs={4} key={selectProject.id}><ProjectCard store={this.props.store} project={selectProject} onTaskSelect={this.onTaskSelect} expanded={true} /></Grid>
            <Grid item xs={8} key="taskStream">
              {/*<TraceTable store={this.props.store} service={selectServiceDescriptor.service} onSelect={this.onTraceSelect} />*/}
            </Grid>
         </Grid>
      ):(
      <Grid container spacing={24}>
          {projectList.map( (x: Project) => <Grid item xs={4} key={x.id}><ProjectCard project={x} store={this.props.store} onTaskSelect={this.onTaskSelect} onProjectSelect={this.onProjectSelect} /></Grid> )}
      </Grid>)}
      </div>);       
  }
}

export default withRoot(withStyles(styles)(Projects));