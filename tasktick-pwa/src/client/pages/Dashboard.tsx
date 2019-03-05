import * as React from 'react';
import {observer} from 'mobx-react';
import { Theme } from '@material-ui/core/styles/createMuiTheme';
import createStyles from '@material-ui/core/styles/createStyles';
import withStyles, { WithStyles } from '@material-ui/core/styles/withStyles';
import Paper from '@material-ui/core/Paper';
import Grid from '@material-ui/core/Grid';
import withRoot from '../withRoot';
import TraceTable from '../components/TraceTable';

const styles = (theme: Theme) =>
  createStyles({
    root: {
      
    },
    tableContainer: {
      height: 320,
    },
    paper: {
      padding: 0,
      textAlign: 'center',
      color: theme.palette.text.secondary,
    },
  });

type State = {
  
};

interface Props {
  store: any;
};

@observer
class Dashboard extends React.Component<Props & WithStyles<typeof styles>, State> {
  state = {
    selectedTrace: undefined,
    selectedTraces: []
  };


  render() {    
    //const eventStream = this.props.store.eventStreamStore.events
    const  classes = this.props.classes;
    return (
      <div className={this.props.classes.root}> 
      <Grid container spacing={24}>
        <Grid item xs={12}>
          <Paper className={classes.paper}>
            
          </Paper>
        </Grid>        
        <Grid item xs={6}>
          TODO
        </Grid>
        <Grid item xs={6}>
          TODO    
        </Grid>        
      </Grid>              
      </div>
    );
  }
}

export default withRoot(withStyles(styles)(Dashboard));