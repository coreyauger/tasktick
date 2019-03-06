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
import ShareIcon from '@material-ui/icons/Share';
import ExpandMoreIcon from '@material-ui/icons/ExpandMore';
import MoreVertIcon from '@material-ui/icons/MoreVert';
import List from '@material-ui/core/List';
import ListItem from '@material-ui/core/ListItem';
import ListItemText from '@material-ui/core/ListItemText';
import CardActionArea from '@material-ui/core/CardActionArea';
import { Project, Task } from '../stores/data';
import { TextField, Button, Grid, Checkbox } from '@material-ui/core';
import { observer } from 'mobx-react';

const styles = (theme: Theme) =>
  createStyles({
    card: {
        maxWidth: 800,
      },
      thin:{
        width: "75%"
      },
      media: {
        height: 0,
        paddingTop: '56.25%', // 16:9
      },
      actions: {
        display: 'flex',
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
      strike: {
        textDecoration: "line-through",
      },
      button: {
        margin: theme.spacing.unit,
        float: "right"
      },
  });

type State = {
    expanded: boolean
    taskName: string
};

interface Props {
  store: any;
  project: Project;  
  expanded?: boolean;
  onTaskSelect: (Task) => void;
  onProjectSelect?: (Project) => void;
};

@observer
class ProjectCard extends React.Component<Props & WithStyles<typeof styles>, State> {
    state = { expanded: this.props.expanded, taskName: "" };

    componentDidMount(){
      console.log("MOUNT PROJECT>..")
      this.props.store.socketStore.socket.send("GetProject", {id: this.props.project.id})  
    }
    componentDidUpdate(prevProps){
      if(prevProps.project.id != this.props.project.id){
        // refresh the project..
        console.log("REFRESH PROJECT>..")
        this.props.store.socketStore.socket.send("GetProject", {id: this.props.project.id})        
      }
    }

    handleExpandClick = () => {
      this.setState(state => ({ expanded: !state.expanded }));
    };
    onTaskSelect = (x: Task) => {
        this.props.onTaskSelect(x);
    }
    selectServiceType = () =>{
        if(this.props.project)
            this.props.onProjectSelect(this.props.project)
    }
    updateTaskName = (event) => {
      this.setState({ taskName: event.target.value });
    };
    addTask = () =>{
      //project: UUID, name: String, description: String, section: String, parent: Option[UUID] = None
      this.props.store.socketStore.socket.send("NewTask", {project: this.props.project.id, name: this.state.taskName, description: "", section: "Default"})  
      this.setState({taskName: ""})
    }
    toggleTaskDone = (t: Task) => () =>{
      t.done=!t.done
      this.props.store.socketStore.socket.send("EditTask", {task: t})  
    }

  render() {    
    const classes = this.props.classes 
    const sd = this.props.project
    //console.log("Project !!!:", this.props.store.taskStore.tasks)    
    return (
        <Card className={classes.card}>          
        <CardHeader
          avatar={
            <Avatar aria-label="Service" className={classes.avatar}>
              P
            </Avatar>
          }
          action={
            <IconButton>
              <MoreVertIcon />
            </IconButton>
          }
          title={sd.name}
          subheader={sd.description}
        />
        <CardActionArea onClick={this.selectServiceType}>
        <CardMedia
          className={classes.media}
          image={"/img/bg"+sd.id[0]+".png"}
          title={sd.name}
        />
        <CardContent>
          <Typography component="p">
            {sd.description}            
          </Typography>
        </CardContent>
        </CardActionArea>
        <CardActions className={classes.actions} disableActionSpacing>
          <IconButton aria-label="Add to favorites">
            <FavoriteIcon />
          </IconButton>        
          <IconButton
            className={classnames(classes.expand, {
              [classes.expandOpen]: this.state.expanded,
            })}
            onClick={this.handleExpandClick}
            aria-expanded={this.state.expanded}
            aria-label="Show more"
          >
            <ExpandMoreIcon />
          </IconButton>
        </CardActions>
        <Collapse in={this.state.expanded} timeout="auto" unmountOnExit>
          <CardContent>
            <Typography paragraph>Tasks:</Typography>
            <div>      
                <Button color="secondary" className={classes.button} disabled={this.state.taskName == ""} onClick={this.addTask}>Add</Button>     
                <div className={classes.thin}>                    
                < TextField onChange={this.updateTaskName} value={this.state.taskName} autoFocus margin="dense" id="task" label="New Task" type="text" fullWidth  />
                </div>  
                         
            </div>
            <div>
                <List>
                {sd.tasks.map(x => this.props.store.taskStore.tasks[x] ).filter(x => x).map( (x: Task ) => (
                    <ListItem key={x.id} button={true} onClick={() => this.onTaskSelect(x) } >
                        <Checkbox
                            checked={x.done}
                            tabIndex={-1}    
                            onChange={this.toggleTaskDone(x)}                      
                          />
                        <ListItemText primary={x.name} secondary={x.description} className={x.done && classes.strike} />
                    </ListItem>)                
                )}
              </List> 
            </div>
          </CardContent>
        </Collapse>
      </Card>
    );
  }
}

export default withRoot(withStyles(styles)(ProjectCard));