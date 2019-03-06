import * as React from 'react';
import { Theme } from '@material-ui/core/styles/createMuiTheme';
import classnames from 'classnames';
import createStyles from '@material-ui/core/styles/createStyles';
import withStyles, { WithStyles } from '@material-ui/core/styles/withStyles';
import withRoot from '../withRoot';
import Card from '@material-ui/core/Card';
import CardHeader from '@material-ui/core/CardHeader';
import CardMedia from '@material-ui/core/CardMedia';
import CardContent from '@material-ui/core/CardContent';
import CardActions from '@material-ui/core/CardActions';
import Collapse from '@material-ui/core/Collapse';
import Avatar from '@material-ui/core/Avatar';
import IconButton from '@material-ui/core/IconButton';
import Typography from '@material-ui/core/Typography';
import red from '@material-ui/core/colors/red';
import FavoriteIcon from '@material-ui/icons/Favorite';
import ExpandMoreIcon from '@material-ui/icons/ExpandMore';
import MoreVertIcon from '@material-ui/icons/MoreVert';
import List from '@material-ui/core/List';
import ListItem from '@material-ui/core/ListItem';
import ListItemText from '@material-ui/core/ListItemText';
import CardActionArea from '@material-ui/core/CardActionArea';
import { Project, Task, Note } from '../stores/data';
import { TextField, Button, Grid, Checkbox } from '@material-ui/core';
import { observer } from 'mobx-react';

const styles = (theme: Theme) =>
  createStyles({
    card: {
        maxWidth: 800,
      },
      media: {
        height: 0,
        paddingTop: '56.25%', // 16:9
      },
      actions: {
        display: 'flex',
      },
      textField: {
        marginLeft: theme.spacing.unit,
        marginRight: theme.spacing.unit,
        width: 200,
      },
      expand: {
        transform: 'rotate(0deg)',
        transition: theme.transitions.create('transform', {
          duration: theme.transitions.duration.shortest,
        }),
        marginLeft: 'auto',
        [theme.breakpoints.up('sm')]: {
          marginRight: -8,
        },
      },
      expandOpen: {
        transform: 'rotate(180deg)',
      },
      avatar: {
        backgroundColor: red[500],
      },
      slim: {
        width: "75%"
      },
      button: {
        margin: theme.spacing.unit,
        float: "right"
      },
  });

type State = {
    name: string
    note: string
    description: string    
    startDate?: number
    endDate?: number
};

interface Props {
  store: any;
  task: Task;  
  //onNoteAdd: (Note) => void;  
};

@observer
class TaskCard extends React.Component<Props & WithStyles<typeof styles>, State> {
    state = { name: this.props.task.name, note: "", description: this.props.task.description, startDate: this.props.task.startDate, endDate: this.props.task.endDate };

    componentDidUpdate(prevProps){
      if(prevProps.task.id != this.props.task.id){
        this.setState({ name: this.props.task.name, note: "", description: this.props.task.description, startDate: this.props.task.startDate, endDate: this.props.task.endDate });
      }
    }

    updateTaskName = (event) => {
      this.setState({ name: event.target.value });
    };
    updateEndDate = (event) => {
      const time = new Date(event.target.value).getTime()
      this.setState({ endDate: time });      
    };
    updateStartDate = (event) => {
      const time = new Date(event.target.value).getTime()
      this.setState({ startDate: time });      
    };
    updateTaskDescription = (event) => {
      this.setState({ description: event.target.value });
    };
    updateNoteName= (event) => {
      this.setState({ note: event.target.value });
    };  
    toggleTaskDone = (t: Task) => () =>{
      t.done=!t.done
      this.updateTask(t);
    }
    addNote = () =>{      
      this.props.store.socketStore.socket.send("NewNote", {task: this.props.task.id, project: this.props.task.project, note: this.state.note})        
      this.setState({note: ""})
    }
    updateTask = (t: Task) =>{
      this.props.store.socketStore.socket.send("EditTask", {task: t})  
    }
    saveTask = () =>{
      let t = this.props.task
      t.name = this.state.name
      t.description = this.state.description
      t.endDate = this.state.endDate
      t.startDate = this.state.startDate
      console.log("T", t)
      this.updateTask(t)
    }

  render() {    
    const classes = this.props.classes 
    const sd = this.props.task
    //console.log("Project !!!:", this.props.store.taskStore.tasks)    
    return (
        <Card className={classes.card}>          
        <CardHeader
          avatar={
            <Avatar aria-label="Service" className={classes.avatar}>
              T
            </Avatar>
          }
          action={
            <Checkbox
                checked={sd.done}
                tabIndex={-1}    
                onChange={this.toggleTaskDone(sd)}                      
              />
          }
          title={sd.name}
          subheader={sd.description}
        />    
        <CardContent>
          <Typography component="p">
            {sd.description}            
          </Typography>
        </CardContent>        
        <CardContent>
          <div>            
              <TextField onChange={this.updateTaskName} value={this.state.name} autoFocus margin="dense" id="task" label="Task" type="text" fullWidth  />
              <TextField onChange={this.updateTaskDescription} value={this.state.description} margin="dense" id="task" label="Description" type="text" fullWidth  />
              <TextField
                  id="date-start"
                  label="Start Date"
                  type="date"
                  value={this.state.startDate ? new Date(this.state.startDate).toDateString() : undefined }
                  className={classes.textField}
                  InputLabelProps={{
                    shrink: true,
                  }}
                />
                <TextField
                  id="date-end"
                  label="End Date"
                  type="date"
                  value={this.state.endDate ? new Date(this.state.startDate).toDateString() : undefined }
                  onChange={this.updateEndDate}
                  className={classes.textField}
                  InputLabelProps={{
                    shrink: true,
                  }}
                />
          </div>
          <div>
          <Typography paragraph>Notes:</Typography>  
          <div>            
                <Button color="secondary" className={classes.button} disabled={this.state.note == ""} onClick={this.addNote}>Add</Button>              
                <div className={classes.slim}><TextField onChange={this.updateNoteName} value={this.state.note} margin="dense" id="note" label="New Note" type="text" fullWidth  /></div>                
            </div>        
          <List>
              {sd.notes.filter(x => x).map( (x: Note ) => (
                  <ListItem key={x.id} button={true} >                        
                      <ListItemText primary={x.note} secondary={x.date} />
                  </ListItem>)                
              )}
              </List> 
          </div>
          <div>
              <Button color="secondary" className={classes.button} onClick={this.saveTask}>Save</Button>              
          </div>
        </CardContent>
      
      </Card>
    );
  }
}

export default withRoot(withStyles(styles)(TaskCard));