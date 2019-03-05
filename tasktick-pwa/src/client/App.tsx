import * as React from 'react';
import { Route } from 'react-router';
import classNames from 'classnames';
import {observer, inject} from 'mobx-react';
import withStyles, { WithStyles } from '@material-ui/core/styles/withStyles';
import CssBaseline from '@material-ui/core/CssBaseline';
import Drawer from '@material-ui/core/Drawer';
import AppBar from '@material-ui/core/AppBar';
import Toolbar from '@material-ui/core/Toolbar';
import List from '@material-ui/core/List';
import Typography from '@material-ui/core/Typography';
import Divider from '@material-ui/core/Divider';
import IconButton from '@material-ui/core/IconButton';
import Badge from '@material-ui/core/Badge';
import MenuIcon from '@material-ui/icons/Menu';
import ChevronLeftIcon from '@material-ui/icons/ChevronLeft';
import NotificationsIcon from '@material-ui/icons/Notifications';
import { Theme } from '@material-ui/core/styles/createMuiTheme';
import createStyles from '@material-ui/core/styles/createStyles';
import withRoot from './withRoot';
import stores from './stores/index';

import ListItem from '@material-ui/core/ListItem';
import ListItemIcon from '@material-ui/core/ListItemIcon';
import ListItemText from '@material-ui/core/ListItemText';
import ListSubheader from '@material-ui/core/ListSubheader';
import DashboardIcon from '@material-ui/icons/Dashboard';
import PeopleIcon from '@material-ui/icons/People';
import AddIcon from '@material-ui/icons/Add';
import BarChartIcon from '@material-ui/icons/BarChart';
import LayersIcon from '@material-ui/icons/Layers';
import AssignmentIcon from '@material-ui/icons/Assignment';
import Dashboard from './pages/Dashboard';
import { Dialog, DialogTitle, DialogContent, DialogContentText, TextField, DialogActions, Button } from '@material-ui/core';
import {TasktickSocket} from './socket/WebSocket'
import SignIn from './pages/SignIn';
import { Fab } from '@material-ui/core';
import Projects from './pages/Projects';
import { uuidv4 } from './stores/data';



const drawerWidth = 240;

const styles = (theme: Theme) =>
createStyles({
  root: {
    display: 'flex',
  },
  toolbar: {
    paddingRight: 24, // keep right padding when drawer closed
  },
  fab: {
    margin: theme.spacing.unit,
  },
  toolbarIcon: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'flex-end',
    padding: '0 8px',
    ...theme.mixins.toolbar,
  },
  appBar: {
    zIndex: theme.zIndex.drawer + 1,
    transition: theme.transitions.create(['width', 'margin'], {
      easing: theme.transitions.easing.sharp,
      duration: theme.transitions.duration.leavingScreen,
    }),
  },
  appBarShift: {
    marginLeft: drawerWidth,
    width: `calc(100% - ${drawerWidth}px)`,
    transition: theme.transitions.create(['width', 'margin'], {
      easing: theme.transitions.easing.sharp,
      duration: theme.transitions.duration.enteringScreen,
    }),
  },
  menuButton: {
    marginLeft: 12,
    marginRight: 36,
  },
  menuButtonHidden: {
    display: 'none',
  },
  title: {
    flexGrow: 1,
  },
  drawerPaper: {
    position: 'relative',
    whiteSpace: 'nowrap',
    width: drawerWidth,
    transition: theme.transitions.create('width', {
      easing: theme.transitions.easing.sharp,
      duration: theme.transitions.duration.enteringScreen,
    }),
  },
  drawerPaperClose: {
    overflowX: 'hidden',
    transition: theme.transitions.create('width', {
      easing: theme.transitions.easing.sharp,
      duration: theme.transitions.duration.leavingScreen,
    }),
    width: theme.spacing.unit * 7,
    [theme.breakpoints.up('sm')]: {
      width: theme.spacing.unit * 9,
    },
  },
  appBarSpacer: theme.mixins.toolbar,
  content: {
    flexGrow: 1,
    padding: theme.spacing.unit * 3,
    height: '100vh',
    overflow: 'auto',    
    backgroundColor: "#f1f0f1",
  },  
  h5: {
    marginBottom: theme.spacing.unit * 2,
  },
  logo:{ 
      mark: {
        width: 36,
        height: 36,        
    }
  }
});

export const secondaryListItems = (
  <div>
    <ListSubheader inset>Projects</ListSubheader>
    <ListItem button>
      <ListItemIcon>
        <AssignmentIcon />
      </ListItemIcon>
      <ListItemText primary="Some Project" />
    </ListItem>
    <ListItem button>
      <ListItemIcon>
        <AssignmentIcon />
      </ListItemIcon>
      <ListItemText primary="Another Project" />
    </ListItem>
  </div>
);


type State = {
  open: boolean;
  name: string;
  description: string;
  showAddProject: boolean;
};

type Props = {
  store: any
}

@inject('routing')
@observer
class App extends React.Component<Props & WithStyles<typeof styles>, State> {
  state = {
    open: true,
    name: "",
    description: "",
    showAddProject: false
  };
  constructor(props){    
    super(props);
    if(window.localStorage.getItem("authToken"))
      props.store.socketStore.connect(new TasktickSocket(window.localStorage.getItem("authToken")))    
  }

  handleDrawerOpen = () => {
    this.setState({ open: true });
  };

  handleDrawerClose = () => {
    this.setState({ open: false });
  };
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
  }

  render() {
    console.log("APP RENDER!!")
    const { classes } = this.props;    
    const { location, push, goBack } = stores.routing; // CA - inject above did not work.. you should see this as a "prop" (investigate)
    return (
      
      <React.Fragment>
        <CssBaseline />
        <div className={classes.root}>      
          <AppBar          
            position="absolute"
            className={classNames(classes.appBar, this.state.open && classes.appBarShift)}
          >            
            <Toolbar disableGutters={!this.state.open} className={classes.toolbar}>
            
              <IconButton
                color="inherit"
                aria-label="Open drawer"
                onClick={this.handleDrawerOpen}
                className={classNames(
                  classes.menuButton,
                  this.state.open && classes.menuButtonHidden,
                )}
              >               
                <MenuIcon />              
              </IconButton>
              <Typography
                component="h1"
                variant="h6"
                color="inherit"
                noWrap
                className={classes.title}
              >
                Dashboard
              </Typography>
              <IconButton color="inherit">
                <Badge badgeContent={4} color="secondary">
                  <NotificationsIcon />
                </Badge>
              </IconButton>
            </Toolbar>
          </AppBar>
          <Drawer
            variant="permanent"
            classes={{
              paper: classNames(classes.drawerPaper, !this.state.open && classes.drawerPaperClose),
            }}
            open={this.state.open}>
            <div className={classes.toolbarIcon}>
              <img src="/imgs/typebus-logo.png" alt="logo" style={ {"width":"66%"} } />            
              
              <IconButton onClick={this.handleDrawerClose}>
                <ChevronLeftIcon />
              </IconButton>
            </div>
            <Divider />
           <List>
           <div>
            <ListItem button>
              <ListItemIcon>
                <DashboardIcon />
              </ListItemIcon>
              <ListItemText primary="Dashboard" onClick={() => push("/p/home")} />
            </ListItem>                        
            <ListItem button>
              <ListItemIcon>
                <LayersIcon />
              </ListItemIcon>
              <ListItemText primary="Projects" onClick={() => push("/p/projects")} />
            </ListItem>
            <ListItem button>
              <ListItemIcon>
                <BarChartIcon />
              </ListItemIcon>
              <ListItemText primary="Metrics" onClick={() => push("/p/tests")} />
            </ListItem>
          </div>
           </List>
            <Divider />
            <List>{secondaryListItems}</List> 
          </Drawer>
          <main className={classes.content}>
            <div className={classes.appBarSpacer} />


            <Route path='/p/signin' render={(props) => <SignIn store={stores} /> }  />
            <Route path='/p/home' render={(props) => <Dashboard store={stores} /> }  />
            <Route path='/p/project/:id' render={(props) => <Projects store={stores} project={props.id} /> }  />
            <Route path='/p/projects' render={(props) => <Projects store={stores} /> }  />                        

            <Fab color="primary" aria-label="Add" className={classes.fab} onClick={this.showAddProject} >
              <AddIcon />
            </Fab>
          </main>
        </div>
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
      </React.Fragment>
    );
  }
}

export default withRoot((withStyles(styles)(App) ));
