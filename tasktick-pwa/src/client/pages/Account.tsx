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
import { Dialog, DialogTitle, DialogContent, DialogContentText, TextField, DialogActions, Button, Card, CardActionArea, CardMedia, CardContent, Typography, CardActions, Avatar, CardHeader } from '@material-ui/core';
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
    button: {
      margin: theme.spacing.unit,
      float: "right"
    },
    media: {
      height: 340,
    },
  });

type State = {
  
};

interface Props {
  store: any;    
};

@observer
class Account extends React.Component<Props & WithStyles<typeof styles>, State> {
  state = {
    
  };

  linkGithub = () => {
    window.open('/api/auth/github', '_blank');
  }
 
  render() {    
    const { location, push, goBack } = this.props.store.routing; // CA - inject above did not work.. you should see this as a "prop" (investigate)
    const user = this.props.store.userStore.users.length && this.props.store.userStore.users[0]
    const pathname = location.pathname.split('/')    
    const  classes = this.props.classes;
    return (<div className={this.props.classes.root}>
        <Grid container spacing={24}> 
          <Grid item xs={4} key={"user"}>
          <Card>
            <CardActionArea>
              <CardMedia
                className={classes.media}
                image="https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcR4Je1-yXBrbsfj-WVPDyMsgGGSe3_qUmHbo-pfRs4x7NOcNBvD"
                title="Avatar"
              />
              <CardContent>
                <Typography gutterBottom variant="h5" component="h2">
                  {user && user.firstName} {user && user.lastName} 
                </Typography>
                <Typography component="p">
                {user && user.email} 
                </Typography>
              </CardContent>
            </CardActionArea>
            <CardActions>
              <Button size="small" color="primary" onClick={this.linkGithub}>
                Link GitHub
              </Button>           
            </CardActions>
          </Card>
          </Grid>
          <Grid item xs={8} key="userStream">


          <Card >
          <CardHeader
          avatar={
            <Avatar aria-label="Service">
              U
            </Avatar>
          }          
          title={"Account Information"}
          subheader={"update your account information"}
        />    
        <CardContent>
          <Typography component="p">
            {"this is some text."}            
          </Typography>
        </CardContent>        
        <CardContent>
          <div>            
              <TextField  autoFocus value={user && user.firstName} margin="dense" id="task" label="Full Name" type="text" fullWidth  />
              <TextField margin="dense" value={user && user.email} id="task" label="Email address" type="text" fullWidth  />            
          </div>       
          <div>
              <Button color="secondary" className={classes.button}>Save</Button>              
          </div>
        </CardContent>
          </Card>

          </Grid>
        </Grid>     
      </div>);       
  }
}

export default withRoot(withStyles(styles)(Account));