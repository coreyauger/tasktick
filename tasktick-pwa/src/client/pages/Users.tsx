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
import { Dialog, DialogTitle, DialogContent, DialogContentText, TextField, DialogActions, Button, Card, CardActionArea, CardMedia, CardContent, Typography, CardActions } from '@material-ui/core';
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
    card: {
      maxWidth: 345,
    },
    media: {
      height: 140,
    },
  });

type State = {
  
};

interface Props {
  store: any;    
};

@observer
class Users extends React.Component<Props & WithStyles<typeof styles>, State> {
  state = {
    
  };
 
  render() {    
    const { location, push, goBack } = this.props.store.routing; // CA - inject above did not work.. you should see this as a "prop" (investigate)
    const pathname = location.pathname.split('/')    
    const  classes = this.props.classes;
    return (<div className={this.props.classes.root}>
        <Grid container spacing={24}> 
          <Grid item xs={4} key={"user"}>         
          </Grid>
          <Grid item xs={8} key="userStream">
          </Grid>
        </Grid>     
      </div>);       
  }
}

export default withRoot(withStyles(styles)(Users));