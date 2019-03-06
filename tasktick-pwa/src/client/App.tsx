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
import PeopleIcon from '@material-ui/icons/People';
import BarChartIcon from '@material-ui/icons/BarChart';
import LayersIcon from '@material-ui/icons/Layers';
import AssignmentIcon from '@material-ui/icons/Assignment';

import {TasktickSocket} from './socket/WebSocket'
import SignIn from './pages/SignIn';
import Projects from './pages/Projects';
import { uuidv4, Project } from './stores/data';
import { Menu, MenuItem } from '@material-ui/core';



const drawerWidth = 240;

const styles = (theme: Theme) =>
createStyles({
  root: {
    display: 'flex',
  },
  toolbar: {
    paddingRight: 24, // keep right padding when drawer closed
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

type State = {
  open: boolean;
  anchorEl: any;
};

type Props = {
  store: any
}

@inject('routing')
@observer
class App extends React.Component<Props & WithStyles<typeof styles>, State> {
  state = {
    open: true,   
    anchorEl: null
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
  onSelectProject = (id) => () =>{
    const { location, push, goBack } = stores.routing; // CA - inject above did not work.. you should see this as a "prop" (investigate)
    push("/p/project/"+id)
  }

  openMenu = event => {
    this.setState({ anchorEl: event.currentTarget });
  };

  handleClose = () => {
    this.setState({ anchorEl: null });
  };
  handleLogout = () =>{
    window.localStorage.clear();
    if(this.props.store.socketStore.socket)
      this.props.store.socketStore.socket.close()
    this.props.store.clear()
    const { location, push, goBack } = this.props.store.routing; // CA - inject above did not work.. you should see this as a "prop" (investigate)
    this.handleClose();
    push("/p/signin")
  }

  render() {
    console.log("APP RENDER!!")
    const projectList = this.props.store.projectStore.projects
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
                TaskTick
              </Typography>
              <IconButton color="inherit" onClick={this.openMenu}>
                <Badge badgeContent={4} color="secondary">
                  <NotificationsIcon />
                </Badge>               
              </IconButton>
              <Menu
                  id="simple-menu"
                  anchorEl={this.state.anchorEl}
                  open={this.state.anchorEl != null}
                  onClose={this.handleClose}
                >
                  <MenuItem onClick={this.handleClose}>Profile</MenuItem>
                  <MenuItem onClick={this.handleClose}>My account</MenuItem>
                  <MenuItem onClick={this.handleLogout}>Logout</MenuItem>
                </Menu>
            </Toolbar>
          </AppBar>
          <Drawer
            variant="permanent"
            classes={{
              paper: classNames(classes.drawerPaper, !this.state.open && classes.drawerPaperClose),
            }}
            open={this.state.open}>
            <div className={classes.toolbarIcon}>
              <img src="/img/logo.png" alt="logo" style={ {"width":"66%"} } />            
              
              <IconButton onClick={this.handleDrawerClose}>
                <ChevronLeftIcon />
              </IconButton>
            </div>
            <Divider />
           <List>
           <div>                                 
            <ListItem button>
              <ListItemIcon>
                <LayersIcon />
              </ListItemIcon>
              <ListItemText primary="Projects" onClick={() => push("/p/projects")} />
            </ListItem>
            <ListItem button>
              <ListItemIcon>
                <PeopleIcon />
              </ListItemIcon>
              <ListItemText primary="Users" onClick={() => push("/p/users")} />
            </ListItem>            
            <ListItem button>
              <ListItemIcon>
                <BarChartIcon />
              </ListItemIcon>
              <ListItemText primary="Metrics" onClick={() => push("/p/metrics")} />
            </ListItem>
          </div>
           </List>
            <Divider />
            <List>
              <div>
                <ListSubheader inset>Projects</ListSubheader>
                {projectList.map(x => (
                  <ListItem key={"_plis"+x.id} button onClick={this.onSelectProject(x.id)}>
                  <ListItemIcon>
                    <AssignmentIcon />
                  </ListItemIcon>
                  <ListItemText primary={x.name} />
                </ListItem>
                ))}   
              </div>  
            </List> 
          </Drawer>
          <main className={classes.content}>
            <div className={classes.appBarSpacer} />

            <Route path='/p/signin' render={(props) => <SignIn store={stores} /> }  />            
            <Route path='/p/project/:id' render={(props) => <Projects store={stores} project={props.id} /> }  />
            <Route path='/p/projects' render={(props) => <Projects store={stores} /> }  />        
                
            
          </main>
        </div>
        
      </React.Fragment>
    );
  }
}

export default withRoot((withStyles(styles)(App) ));
